package online.larsen.cddl.psi.impl

import com.intellij.lang.ASTNode
import online.larsen.cddl.CddlIcons
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.resolve.CddlRuleKind
import online.larsen.cddl.resolve.kind
import javax.swing.Icon

abstract class CddlRuleMixin(node: ASTNode) : CddlNamedElementBase(node), CddlRule {
    override fun getIcon(flags: Int): Icon =
        if (kind == CddlRuleKind.GROUP) CddlIcons.GROUP_RULE else CddlIcons.TYPE_RULE
}
