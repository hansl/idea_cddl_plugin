package online.larsen.cddl.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

object CddlDocUtil {
    /**
     * The documentation of a rule: the comment lines directly above it (without blank lines in between), or else a
     * comment at the end of its first line.
     */
    fun docComment(rule: CddlRule): String? {
        val lines = mutableListOf<String>()
        var element: PsiElement? = rule.prevSibling
        while (element != null) {
            if (element is PsiWhiteSpace) {
                if (element.text.count { it == '\n' } > 1) break
            } else if (element is PsiComment && element.node.elementType == CddlTypes.COMMENT) {
                lines.add(0, commentText(element))
            } else {
                break
            }
            element = element.prevSibling
        }
        if (lines.isNotEmpty()) return lines.joinToString("\n")

        val trailing = rule.nextSibling.let { if (it is PsiWhiteSpace && '\n' !in it.text) it.nextSibling else it }
        if (trailing is PsiComment && trailing.node.elementType == CddlTypes.COMMENT) return commentText(trailing)
        return null
    }

    fun commentText(comment: PsiComment): String = comment.text.removePrefix(";").trim()
}
