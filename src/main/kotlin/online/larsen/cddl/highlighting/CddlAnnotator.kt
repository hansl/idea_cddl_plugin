package online.larsen.cddl.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.psi.CddlBarewordMemberKey
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlLiteralType
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlStringUtil
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.identifier
import online.larsen.cddl.psi.isTextString
import online.larsen.cddl.psi.literalTokenType
import online.larsen.cddl.psi.referenceName
import online.larsen.cddl.psi.resolveTargets
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlRuleKind
import online.larsen.cddl.resolve.kind
import online.larsen.cddl.resolve.socketKind

/** Semantic highlighting: declarations, references by kind, member keys, string escapes and unterminated strings. */
class CddlAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is CddlRule -> element.nameIdentifier?.let {
                val key = if (element.kind == CddlRuleKind.GROUP) {
                    CddlHighlighterColors.GROUP_DECLARATION
                } else {
                    CddlHighlighterColors.TYPE_DECLARATION
                }
                highlight(holder, it, key)
            }
            is CddlGenericParam -> element.nameIdentifier?.let {
                highlight(holder, it, CddlHighlighterColors.GENERIC_PARAMETER)
            }
            is CddlNameRef -> referenceAttributes(element)?.let { highlight(holder, element, it) }
            is CddlBarewordMemberKey -> highlight(holder, element.identifier, CddlHighlighterColors.MEMBER_KEY)
            is CddlLiteralType -> annotateLiteral(element, holder)
        }
    }

    private fun referenceAttributes(ref: CddlNameRef): TextAttributesKey? {
        val target = ref.resolveTargets.firstOrNull()
            ?: return when (socketKind(ref.referenceName)) {
                CddlRuleKind.GROUP -> CddlHighlighterColors.GROUP_REFERENCE
                CddlRuleKind.TYPE -> CddlHighlighterColors.TYPE_REFERENCE
                else -> null
            }
        return when {
            target is CddlGenericParam -> CddlHighlighterColors.GENERIC_PARAMETER
            CddlPrelude.isPrelude(target) -> CddlHighlighterColors.PRELUDE_TYPE
            target is CddlRule && target.kind == CddlRuleKind.GROUP -> CddlHighlighterColors.GROUP_REFERENCE
            else -> CddlHighlighterColors.TYPE_REFERENCE
        }
    }

    private fun annotateLiteral(literal: CddlLiteralType, holder: AnnotationHolder) {
        if (literal.literalTokenType !in CddlTokenSets.STRINGS) return
        val text = literal.text
        if (!CddlStringUtil.isTerminated(text)) {
            holder.newAnnotation(HighlightSeverity.ERROR, CddlBundle.message("annotator.unterminated.string"))
                .range(literal)
                .create()
        }
        if (!literal.isTextString) return

        val content = CddlStringUtil.contentRange(text)
        val decoded = CddlStringUtil.decode(content.substring(text))
        val base = literal.textRange.startOffset + content.startOffset
        for (escape in decoded.validEscapes) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(escape.shiftRight(base))
                .textAttributes(CddlHighlighterColors.VALID_ESCAPE)
                .create()
        }
        for (escape in decoded.invalidEscapes) {
            holder.newAnnotation(HighlightSeverity.WARNING, CddlBundle.message("annotator.invalid.escape"))
                .range(escape.shiftRight(base))
                .textAttributes(CddlHighlighterColors.INVALID_ESCAPE)
                .create()
        }
    }

    private fun highlight(holder: AnnotationHolder, element: PsiElement, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(key).create()
    }
}
