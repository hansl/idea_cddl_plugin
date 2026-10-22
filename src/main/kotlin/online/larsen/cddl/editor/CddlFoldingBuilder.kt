package online.larsen.cddl.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.psi.CddlArrayType
import online.larsen.cddl.psi.CddlEnumType
import online.larsen.cddl.psi.CddlMapType
import online.larsen.cddl.psi.CddlParenType
import online.larsen.cddl.psi.CddlTypes

/** Folds multi-line maps, arrays and parenthesized groups, and runs of line comments. */
class CddlFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        PsiTreeUtil.processElements(root) { element ->
            when (element) {
                is CddlMapType, is CddlArrayType, is CddlParenType, is CddlEnumType -> foldBrackets(element, document)
                is PsiComment -> foldComments(element, document)
                else -> null
            }?.let(descriptors::add)
            true
        }
        return descriptors.toTypedArray()
    }

    private fun foldBrackets(element: PsiElement, document: Document): FoldingDescriptor? {
        val open = element.node.findChildByType(OPENING) ?: return null
        val close = element.node.lastChildNode?.takeIf { it.elementType in CLOSING } ?: return null
        val range = TextRange(open.startOffset, close.textRange.endOffset)
        if (!spansLines(range, document)) return null
        return FoldingDescriptor(element.node, range, null, "${open.text}...${close.text}")
    }

    private fun foldComments(comment: PsiComment, document: Document): FoldingDescriptor? {
        if (comment.node.elementType != CddlTypes.COMMENT || isLineComment(previousComment(comment))) return null
        var last: PsiComment = comment
        while (true) {
            val next = nextComment(last) ?: break
            if (next.node.elementType != CddlTypes.COMMENT) break
            last = next
        }
        if (last == comment) return null
        val range = TextRange(comment.textRange.startOffset, last.textRange.endOffset)
        if (!spansLines(range, document)) return null
        return FoldingDescriptor(comment.node, range, null, comment.text.take(60).trimEnd() + " ...")
    }

    /** The comment on the line right before (or after) [comment], if it is only separated by a single line break. */
    private fun previousComment(comment: PsiComment): PsiComment? = adjacentComment(comment.prevSibling) { it.prevSibling }

    private fun nextComment(comment: PsiComment): PsiComment? = adjacentComment(comment.nextSibling) { it.nextSibling }

    private fun adjacentComment(start: PsiElement?, step: (PsiElement) -> PsiElement?): PsiComment? {
        var element = start
        if (element is PsiWhiteSpace) {
            if (element.text.count { it == '\n' } > 1) return null
            element = step(element)
        }
        return element as? PsiComment
    }

    private fun isLineComment(comment: PsiComment?): Boolean = comment?.node?.elementType == CddlTypes.COMMENT

    private fun spansLines(range: TextRange, document: Document): Boolean =
        document.getLineNumber(range.startOffset) < document.getLineNumber(range.endOffset)

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private companion object {
        val OPENING = TokenSet.create(CddlTypes.LBRACE, CddlTypes.LBRACKET, CddlTypes.LPAREN)
        val CLOSING = TokenSet.create(CddlTypes.RBRACE, CddlTypes.RBRACKET, CddlTypes.RPAREN)
    }
}
