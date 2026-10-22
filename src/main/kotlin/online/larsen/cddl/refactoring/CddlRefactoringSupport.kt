package online.larsen.cddl.refactoring

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.lexer.CddlLexer
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlNamedElement
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.CddlTypes
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlResolver
import online.larsen.cddl.resolve.CddlRuleKind
import online.larsen.cddl.resolve.kind

class CddlFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner =
        DefaultWordsScanner(CddlLexer(), TokenSet.create(CddlTypes.ID), CddlTokenSets.COMMENTS, CddlTokenSets.STRINGS)

    override fun canFindUsagesFor(element: PsiElement): Boolean = element is CddlNamedElement

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is CddlRule -> if (element.kind == CddlRuleKind.GROUP) {
            CddlBundle.message("element.type.group")
        } else {
            CddlBundle.message("element.type.type")
        }
        is CddlGenericParam -> CddlBundle.message("element.type.generic.parameter")
        else -> ""
    }

    override fun getDescriptiveName(element: PsiElement): String = (element as? PsiNamedElement)?.name ?: ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)
}

class CddlNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean = false

    override fun isIdentifier(name: String, project: Project?): Boolean = ID.matches(name)

    companion object {
        val ID = Regex("""[a-zA-Z@_$]([-.]*[a-zA-Z@_$0-9])*""")
    }
}

class CddlRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element is CddlNamedElement

    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element is CddlGenericParam
}

/** Renaming a rule also renames its other definitions (`/=` and `//=` extensions, or the rule they extend). */
class CddlRenameRuleProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is CddlRule

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val rule = element as? CddlRule ?: return
        val name = rule.name ?: return
        val file = rule.containingFile as? CddlFile ?: return
        for (other in CddlResolver.resolveRule(file, name)) {
            if (other != rule && !CddlPrelude.isPrelude(other)) allRenames[other] = newName
        }
    }
}

/** The prelude is fixed: its rules cannot be renamed. */
class CddlPreludeVetoRenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement): Boolean = element is CddlRule && CddlPrelude.isPrelude(element)
}
