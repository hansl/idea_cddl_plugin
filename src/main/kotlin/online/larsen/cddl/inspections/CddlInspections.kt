package online.larsen.cddl.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.CddlControlOperators
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlControlType
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlGenericArgs
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlVisitor
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.psi.assignmentToken
import online.larsen.cddl.psi.genericArgs
import online.larsen.cddl.psi.genericParamList
import online.larsen.cddl.psi.operatorName
import online.larsen.cddl.psi.operatorToken
import online.larsen.cddl.psi.referenceName
import online.larsen.cddl.psi.resolveTargets
import online.larsen.cddl.psi.typeList
import online.larsen.cddl.psi.value
import online.larsen.cddl.resolve.CddlDirectives
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlResolver
import online.larsen.cddl.resolve.CddlRuleKind
import online.larsen.cddl.resolve.kind

/**
 * Reports names that do not resolve to any rule, generic parameter or prelude type. Sockets (`$name`, `$$name`) are
 * exempt: a socket without any plug is legal and simply empty (RFC 8610, section 3.9).
 */
class CddlUnresolvedReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : CddlVisitor() {
        override fun visitNameRef(ref: CddlNameRef) {
            val name = ref.referenceName
            if (name.startsWith("$") || ref.resolveTargets.isNotEmpty()) return
            val file = ref.containingFile as? CddlFile ?: return
            if (CddlDirectives.mayComeFromMissingModule(file, name)) return
            val fixes = mutableListOf<LocalQuickFix>()
            missingSpaceBeforeControlOperator(ref)?.let { fixes += CddlInsertSpaceFix(it) }
            fixes += CddlCreateRuleFix(name)
            holder.registerProblem(
                ref,
                CddlBundle.message("inspection.unresolved.reference.message", name),
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                *fixes.toTypedArray(),
            )
        }
    }

    /**
     * `tstr.size` is a single (undefined) name: a control operator must be separated by a space. Returns the offset
     * of the operator's dot within the name when that is the likely intent.
     */
    private fun missingSpaceBeforeControlOperator(ref: CddlNameRef): Int? {
        val name = ref.referenceName
        val file = ref.containingFile as? CddlFile ?: return null
        var dot = name.indexOf('.')
        while (dot > 0) {
            val operator = name.substring(dot)
            val target = name.substring(0, dot)
            if (CddlControlOperators[operator] != null &&
                (CddlResolver.resolveGenericParam(ref, target) != null || CddlResolver.resolveRule(file, target).isNotEmpty())
            ) {
                return dot
            }
            dot = name.indexOf('.', dot + 1)
        }
        return null
    }
}

/** Reports rules that are never referenced. The first rule of a file (its root) and sockets (`$name`) are exempt. */
class CddlUnusedRuleInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : CddlVisitor() {
        override fun visitRule(rule: CddlRule) {
            val file = rule.containingFile as? CddlFile ?: return
            val name = rule.name ?: return
            if (CddlPrelude.isPrelude(rule) || name.startsWith("$")) return
            val definitions = file.ruleMap[name].orEmpty()
            if (rule != definitions.firstOrNull()) return // report a name once, on its first definition
            if (file.rootRule?.name == name) return
            if (definitions.any { ReferencesSearch.search(it, it.useScope).findFirst() != null }) return
            holder.registerProblem(
                rule.nameIdentifier ?: return,
                CddlBundle.message("inspection.unused.rule.message", name),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                CddlRemoveRuleFix(name),
            )
        }
    }
}

/**
 * Reports names defined more than once with `=` (further choices must be added with `/=` or `//=`), and extensions
 * that do not match the kind of the rule they extend (`/=` on a group, `//=` on a type).
 */
class CddlDuplicateRuleInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : CddlVisitor() {
        override fun visitRule(rule: CddlRule) {
            val file = rule.containingFile as? CddlFile ?: return
            val name = rule.name ?: return
            val identifier = rule.nameIdentifier ?: return
            val definitions = file.ruleMap[name].orEmpty()
            val base = definitions.firstOrNull { it.assignment == CddlAssignment.DEFINE }
            when (rule.assignment) {
                CddlAssignment.DEFINE -> if (base != null && base != rule) {
                    // Specifications extracted from documents often repeat a definition verbatim; that is harmless.
                    if (normalizedValue(rule) == normalizedValue(base)) {
                        holder.registerProblem(
                            identifier,
                            CddlBundle.message("inspection.duplicate.rule.identical.message", name),
                            ProblemHighlightType.WEAK_WARNING,
                        )
                    } else {
                        holder.registerProblem(identifier, CddlBundle.message("inspection.duplicate.rule.message", name))
                    }
                }
                CddlAssignment.TYPE_CHOICE -> if (base?.kind == CddlRuleKind.GROUP) {
                    holder.registerProblem(
                        rule.assignmentToken ?: identifier,
                        CddlBundle.message("inspection.extension.kind.type.message", name),
                    )
                }
                CddlAssignment.GROUP_CHOICE -> if (base?.kind == CddlRuleKind.TYPE) {
                    holder.registerProblem(
                        rule.assignmentToken ?: identifier,
                        CddlBundle.message("inspection.extension.kind.group.message", name),
                    )
                }
            }
        }
    }
}

/** The text of a rule's value without whitespace and comments. */
private fun normalizedValue(rule: CddlRule): String = buildString {
    val value = rule.value ?: return@buildString
    PsiTreeUtil.processElements(value) { element ->
        if (element.firstChild == null && element !is PsiWhiteSpace && element !is PsiComment) append(element.text).append(' ')
        true
    }
}

/** Reports control operators that are not registered with IANA. */
class CddlUnknownControlOperatorInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : CddlVisitor() {
        override fun visitControlType(type: CddlControlType) {
            val name = type.operatorName ?: return
            if (CddlControlOperators[name] != null) return
            holder.registerProblem(
                type.operatorToken ?: return,
                CddlBundle.message("inspection.unknown.control.operator.message", name),
            )
        }
    }
}

/** Reports generic rules used with the wrong number of arguments, and arguments given to non-generic rules. */
class CddlGenericArgumentsInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : CddlVisitor() {
        override fun visitNameRef(ref: CddlNameRef) {
            val targets = ref.resolveTargets
            if (targets.isEmpty() || targets.any { it is CddlGenericParam }) return
            val expected = targets.filterIsInstance<CddlRule>().map { it.genericParamList.size }.distinct()
            val args: CddlGenericArgs? = ref.genericArgs
            val actual = args?.typeList?.size ?: 0
            if (actual in expected) return
            val name = ref.referenceName
            val message = when {
                expected.singleOrNull() == 0 -> CddlBundle.message("inspection.generic.arguments.unexpected", name)
                else -> CddlBundle.message(
                    "inspection.generic.arguments.count", name, expected.joinToString(" or "), actual,
                )
            }
            holder.registerProblem(args ?: ref, message)
        }
    }
}
