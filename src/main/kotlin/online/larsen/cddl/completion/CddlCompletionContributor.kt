package online.larsen.cddl.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.CddlControlOperators
import online.larsen.cddl.CddlIcons
import online.larsen.cddl.psi.CddlDocUtil
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypes
import online.larsen.cddl.psi.containingRule
import online.larsen.cddl.psi.genericParamList
import online.larsen.cddl.psi.referenceName
import online.larsen.cddl.psi.resolveTargets
import online.larsen.cddl.resolve.CddlDirectives
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlRuleNameIndex

class CddlCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val prefix = position.text.substring(0, parameters.offset - position.textRange.startOffset)
        when (position.node.elementType) {
            CddlTypes.CTLOP, CddlTypes.DOT -> completeControlOperators(result.withPrefixMatcher(prefix))
            CddlTypes.ID -> when (val parent = position.parent) {
                is CddlNameRef -> completeNames(parent, parameters, result.withPrefixMatcher(prefix))
                // A new rule's name; before its `=` is typed, the name is parsed as an error at the top level.
                is CddlRule, is CddlFile -> completeUndefinedNames(position.containingFile, result.withPrefixMatcher(prefix))
                is PsiErrorElement -> if (parent.parent is CddlFile) {
                    completeUndefinedNames(position.containingFile, result.withPrefixMatcher(prefix))
                }
            }
        }
    }

    private fun completeControlOperators(result: CompletionResultSet) {
        for (operator in CddlControlOperators.ALL) {
            result.addElement(
                LookupElementBuilder.create(operator.name)
                    .withIcon(CddlIcons.CONTROL_OPERATOR)
                    .withTypeText(operator.reference)
                    .withTailText(if (operator.standard) null else " (non-standard)", true),
            )
        }
    }

    private fun completeNames(ref: CddlNameRef, parameters: CompletionParameters, result: CompletionResultSet) {
        // Rules are looked up in the original file rather than in the completion copy, so lookups hold valid PSI.
        val file = parameters.originalFile as? CddlFile ?: return
        val project = file.project
        val seen = mutableSetOf<String>()

        fun add(element: LookupElement, priority: Double) {
            if (seen.add(element.lookupString)) result.addElement(PrioritizedLookupElement.withPriority(element, priority))
        }

        ref.containingRule?.genericParamList?.forEach { param ->
            param.name?.let { add(paramLookup(it), PRIORITY_GENERIC_PARAM) }
        }
        for ((name, rules) in file.ruleMap) {
            add(ruleLookup(name, rules.first()), PRIORITY_LOCAL)
        }
        val directives = CddlDirectives.directives(file)
        if (directives.isNotEmpty()) {
            for (name in CddlDirectives.visibleNames(file)) {
                val rule = CddlDirectives.resolve(file, name).firstOrNull() ?: continue
                add(ruleLookup(name, rule), PRIORITY_IMPORTED)
            }
        }
        for (rule in CddlPrelude.getInstance(project).rules) {
            rule.name?.let { add(ruleLookup(it, rule, prelude = true), PRIORITY_PRELUDE) }
        }
        if (directives.isEmpty()) {
            for (name in CddlRuleNameIndex.allNames(project)) {
                if (name in seen || !result.prefixMatcher.prefixMatches(name)) continue
                val rule = CddlRuleNameIndex.findRules(project, name, GlobalSearchScope.projectScope(project)).firstOrNull() ?: continue
                if (rule.containingFile != file) add(ruleLookup(name, rule), PRIORITY_PROJECT)
            }
        }
    }

    /** At a rule's name, suggests the names that are used in the file but not defined anywhere. */
    private fun completeUndefinedNames(file: PsiFile, result: CompletionResultSet) {
        PsiTreeUtil.findChildrenOfType(file, CddlNameRef::class.java)
            .filter { CompletionUtil.DUMMY_IDENTIFIER_TRIMMED !in it.text && it.resolveTargets.isEmpty() }
            .map { it.referenceName }
            .distinct()
            .forEach { name ->
                result.addElement(LookupElementBuilder.create(name).withIcon(CddlIcons.TYPE_RULE).withTypeText("undefined"))
            }
    }

    private fun ruleLookup(name: String, rule: CddlRule, prelude: Boolean = false): LookupElement {
        val params = rule.genericParamList.mapNotNull { it.name }
        val location = if (prelude) "prelude" else rule.containingFile?.name
        var builder = LookupElementBuilder.create(rule, name)
            .withIcon(rule.getIcon(0))
            .withTypeText(location, true)
        if (params.isNotEmpty()) {
            builder = builder.withTailText(params.joinToString(", ", "<", ">"), true).withInsertHandler(GENERIC_ARGS)
        } else if (prelude) {
            CddlDocUtil.docComment(rule)?.let { builder = builder.withTailText("  $it", true) }
        }
        return builder
    }

    private fun paramLookup(name: String): LookupElement =
        LookupElementBuilder.create(name).withIcon(CddlIcons.GENERIC_PARAMETER).withTypeText("generic parameter")

    private companion object {
        const val PRIORITY_GENERIC_PARAM = 100.0
        const val PRIORITY_LOCAL = 90.0
        const val PRIORITY_IMPORTED = 80.0
        const val PRIORITY_PRELUDE = 70.0
        const val PRIORITY_PROJECT = 60.0

        /** Inserts `<>` after a generic rule's name, with the caret between the brackets. */
        val GENERIC_ARGS = InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val offset = context.tailOffset
            if (document.charsSequence.getOrNull(offset) == '<') return@InsertHandler
            document.insertString(offset, "<>")
            context.editor.caretModel.moveToOffset(offset + 1)
        }
    }
}

/** Opens the completion popup when a control operator is started (`.` after a space). */
class CddlTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is CddlFile || charTyped != '.') return Result.CONTINUE
        val offset = editor.caretModel.offset
        if (offset > 0 && editor.document.charsSequence[offset - 1].isWhitespace()) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        }
        return Result.CONTINUE
    }
}
