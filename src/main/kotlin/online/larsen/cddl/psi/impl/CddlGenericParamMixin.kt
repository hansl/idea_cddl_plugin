package online.larsen.cddl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import online.larsen.cddl.CddlIcons
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.containingRule
import javax.swing.Icon

abstract class CddlGenericParamMixin(node: ASTNode) : CddlNamedElementBase(node), CddlGenericParam {
    override fun getIcon(flags: Int): Icon = CddlIcons.GENERIC_PARAMETER

    /** A generic parameter is only visible inside the rule that declares it. */
    override fun getUseScope(): SearchScope = LocalSearchScope(containingRule ?: this)
}
