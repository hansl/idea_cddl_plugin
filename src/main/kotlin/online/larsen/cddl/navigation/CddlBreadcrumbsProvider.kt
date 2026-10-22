package online.larsen.cddl.navigation

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import online.larsen.cddl.CddlLanguage
import online.larsen.cddl.psi.CddlBarewordMemberKey
import online.larsen.cddl.psi.CddlGroupEntry
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypeMemberKey
import online.larsen.cddl.psi.CddlValueMemberKey
import online.larsen.cddl.psi.keyName
import online.larsen.cddl.psi.memberKey

/** Shows the rule and member keys around the caret, e.g. `person > address > street`. */
class CddlBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages(): Array<Language> = arrayOf(CddlLanguage)

    override fun acceptElement(element: PsiElement): Boolean =
        element is CddlRule || (element is CddlGroupEntry && element.memberKey != null)

    override fun getElementInfo(element: PsiElement): String = when (element) {
        is CddlRule -> element.name ?: ""
        is CddlGroupEntry -> when (val key = element.memberKey) {
            is CddlBarewordMemberKey -> key.keyName
            is CddlValueMemberKey -> key.firstChild.text
            is CddlTypeMemberKey -> key.type.text.take(30)
            else -> ""
        }
        else -> ""
    }
}
