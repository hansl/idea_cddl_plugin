package online.larsen.cddl.inspections

import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.psi.CddlElementFactory
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.containingRule

/** Adds `name = any` after the rule containing the unresolved reference and selects `any`. */
class CddlCreateRuleFix(private val name: String) : PsiUpdateModCommandQuickFix() {
    override fun getName(): String = CddlBundle.message("fix.create.rule", name)

    override fun getFamilyName(): String = CddlBundle.message("fix.create.rule.family")

    override fun applyFix(project: Project, element: PsiElement, updater: ModPsiUpdater) {
        val rule = element.containingRule ?: return
        val anchor = trailingComment(rule) ?: rule
        val parent = anchor.parent
        val added = parent.addAfter(CddlElementFactory.createRule(project, "$name = any"), anchor) as CddlRule
        parent.addBefore(CddlElementFactory.createNewline(project), added)
        added.type?.let(updater::select)
    }

    /** A comment on the same line as the end of [rule]. */
    private fun trailingComment(rule: CddlRule): PsiComment? {
        val next = rule.nextSibling
        if (next is PsiComment) return next
        if (next is PsiWhiteSpace && '\n' !in next.text) return next.nextSibling as? PsiComment
        return null
    }
}

/** Turns `tstr.size` (a single name) into `tstr .size` (a control operator). */
class CddlInsertSpaceFix(private val offsetInName: Int) : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): String = CddlBundle.message("fix.insert.space.before.control.operator")

    override fun applyFix(project: Project, element: PsiElement, updater: ModPsiUpdater) {
        val rule = element.containingRule ?: return
        val offsetInRule = element.textRange.startOffset - rule.textRange.startOffset + offsetInName
        val text = rule.text
        val fixed = text.substring(0, offsetInRule) + " " + text.substring(offsetInRule)
        rule.replace(CddlElementFactory.createRule(project, fixed))
    }
}

/** Deletes an unused rule, together with its comment lines. */
class CddlRemoveRuleFix(private val name: String) : PsiUpdateModCommandQuickFix() {
    override fun getName(): String = CddlBundle.message("fix.remove.rule", name)

    override fun getFamilyName(): String = CddlBundle.message("fix.remove.rule.family")

    override fun applyFix(project: Project, element: PsiElement, updater: ModPsiUpdater) {
        val rule = PsiTreeUtil.getParentOfType(element, CddlRule::class.java, false) ?: return
        var first: PsiElement = rule
        while (true) {
            // Only comment lines directly above the rule belong to it, not a comment trailing the previous rule.
            val whitespace = first.prevSibling as? PsiWhiteSpace ?: break
            if (whitespace.text.count { it == '\n' } != 1) break
            val comment = whitespace.prevSibling as? PsiComment ?: break
            val beforeComment = comment.prevSibling
            if (beforeComment != null && (beforeComment !is PsiWhiteSpace || '\n' !in beforeComment.text)) break
            first = comment
        }
        val last = (rule.nextSibling as? PsiWhiteSpace)?.takeIf { it.nextSibling != null } ?: rule
        rule.parent.deleteChildRange(first, last)
    }
}
