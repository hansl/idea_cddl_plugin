package online.larsen.cddl.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.LeafElement
import online.larsen.cddl.psi.CddlLiteralType
import online.larsen.cddl.psi.CddlStringUtil
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.isTextString
import online.larsen.cddl.psi.literalToken
import online.larsen.cddl.psi.literalTokenType

abstract class CddlLiteralTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), CddlLiteralType,
    PsiLanguageInjectionHost {

    override fun isValidHost(): Boolean = isTextString

    override fun updateText(text: String): PsiLanguageInjectionHost {
        (literalToken.node as LeafElement).replaceWithText(text)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> = Escaper(this)

    private class Escaper(host: CddlLiteralTypeMixin) : LiteralTextEscaper<CddlLiteralTypeMixin>(host) {
        private var offsets: IntArray = IntArray(0)

        override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
            val decoded = CddlStringUtil.decode(rangeInsideHost.substring(myHost.text))
            outChars.append(decoded.value)
            offsets = decoded.offsets
            return decoded.invalidEscapes.isEmpty()
        }

        override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
            if (offsetInDecoded < 0 || offsetInDecoded >= offsets.size) return -1
            return (offsets[offsetInDecoded] + rangeInsideHost.startOffset).coerceAtMost(rangeInsideHost.endOffset)
        }

        override fun isOneLine(): Boolean = true
    }
}

class CddlLiteralManipulator : AbstractElementManipulator<CddlLiteralTypeMixin>() {
    override fun handleContentChange(
        element: CddlLiteralTypeMixin,
        range: TextRange,
        newContent: String,
    ): CddlLiteralTypeMixin {
        val newText = range.replace(element.text, newContent)
        return element.updateText(newText) as CddlLiteralTypeMixin
    }

    override fun getRangeInElement(element: CddlLiteralTypeMixin): TextRange =
        if (element.literalTokenType in CddlTokenSets.STRINGS) CddlStringUtil.contentRange(element.text)
        else TextRange(0, element.textLength)
}
