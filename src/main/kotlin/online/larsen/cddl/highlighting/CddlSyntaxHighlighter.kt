package online.larsen.cddl.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import online.larsen.cddl.lexer.CddlLexer
import online.larsen.cddl.psi.CddlTypes.*

class CddlSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = CddlLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        pack(ATTRIBUTES[tokenType])

    private companion object {
        val ATTRIBUTES: Map<IElementType, TextAttributesKey> = buildMap {
            put(COMMENT, CddlHighlighterColors.COMMENT)
            put(DIRECTIVE, CddlHighlighterColors.DIRECTIVE)
            put(NUMBER, CddlHighlighterColors.NUMBER)
            put(TEXT_STRING, CddlHighlighterColors.TEXT_STRING)
            put(BYTE_STRING, CddlHighlighterColors.BYTE_STRING)
            put(ID, CddlHighlighterColors.IDENTIFIER)
            put(CTLOP, CddlHighlighterColors.CONTROL_OPERATOR)
            for (token in listOf(TAG_HEAD, MAJOR_TYPE_HEAD, HASH)) put(token, CddlHighlighterColors.TAG)
            for (token in listOf(QUESTION, STAR, PLUS)) put(token, CddlHighlighterColors.OCCURRENCE)
            for (token in listOf(ASSIGN, TCHOICE_ASSIGN, GCHOICE_ASSIGN)) put(token, CddlHighlighterColors.ASSIGNMENT)
            for (token in listOf(TCHOICE, GCHOICE, ARROW, CARET, INCLUSIVE_RANGE, EXCLUSIVE_RANGE, TILDE, AMPERSAND, DOT)) {
                put(token, CddlHighlighterColors.OPERATOR)
            }
            put(COLON, CddlHighlighterColors.COLON)
            put(COMMA, CddlHighlighterColors.COMMA)
            put(LPAREN, CddlHighlighterColors.PARENTHESES)
            put(RPAREN, CddlHighlighterColors.PARENTHESES)
            put(LBRACE, CddlHighlighterColors.BRACES)
            put(RBRACE, CddlHighlighterColors.BRACES)
            put(LBRACKET, CddlHighlighterColors.BRACKETS)
            put(RBRACKET, CddlHighlighterColors.BRACKETS)
            put(LT, CddlHighlighterColors.ANGLE_BRACKETS)
            put(GT, CddlHighlighterColors.ANGLE_BRACKETS)
            put(TokenType.BAD_CHARACTER, CddlHighlighterColors.BAD_CHARACTER)
        }
    }
}

class CddlSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        CddlSyntaxHighlighter()
}
