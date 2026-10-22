package online.larsen.cddl.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import online.larsen.cddl.CddlLanguage
import online.larsen.cddl.lexer.CddlLexer
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.CddlTypes

class CddlParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CddlLexer()

    override fun createParser(project: Project?): PsiParser = CddlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = CddlTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = CddlTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = CddlTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = CddlFile(viewProvider)

    companion object {
        @JvmField
        val FILE = IFileElementType(CddlLanguage)
    }
}
