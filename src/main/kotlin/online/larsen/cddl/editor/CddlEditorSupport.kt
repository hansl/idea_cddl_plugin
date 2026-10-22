package online.larsen.cddl.editor

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.CddlTypes

class CddlBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean =
        contextType == null || contextType in ALLOWED_BEFORE

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(CddlTypes.LBRACE, CddlTypes.RBRACE, true),
            BracePair(CddlTypes.LBRACKET, CddlTypes.RBRACKET, true),
            BracePair(CddlTypes.LPAREN, CddlTypes.RPAREN, false),
            BracePair(CddlTypes.LT, CddlTypes.GT, false),
        )

        val ALLOWED_BEFORE = TokenSet.orSet(
            CddlTokenSets.COMMENTS,
            TokenSet.create(
                TokenType.WHITE_SPACE, CddlTypes.RBRACE, CddlTypes.RBRACKET, CddlTypes.RPAREN, CddlTypes.GT,
                CddlTypes.COMMA, CddlTypes.TCHOICE, CddlTypes.GCHOICE,
            ),
        )
    }
}

class CddlCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "; "

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

class CddlQuoteHandler : SimpleTokenSetQuoteHandler(CddlTypes.TEXT_STRING, CddlTypes.BYTE_STRING)
