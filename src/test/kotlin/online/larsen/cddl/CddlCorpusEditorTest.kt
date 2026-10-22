package online.larsen.cddl

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import online.larsen.cddl.inspections.CddlDuplicateRuleInspection
import online.larsen.cddl.inspections.CddlGenericArgumentsInspection
import online.larsen.cddl.inspections.CddlUnknownControlOperatorInspection
import online.larsen.cddl.inspections.CddlUnresolvedReferenceInspection
import online.larsen.cddl.inspections.CddlUnusedRuleInspection
import online.larsen.cddl.lexer.CddlLexer
import java.io.File

/**
 * Highlights and reformats every `.cddl` file below the directory named by the `CDDL_CORPUS` environment variable.
 * Fails on any exception, and when reformatting changes anything but whitespace. Does nothing when the variable is
 * not set.
 */
class CddlCorpusEditorTest : BasePlatformTestCase() {
    fun testCorpus() {
        val root = System.getenv("CDDL_CORPUS")?.let(::File) ?: return
        myFixture.enableInspections(
            CddlUnresolvedReferenceInspection(),
            CddlUnusedRuleInspection(),
            CddlDuplicateRuleInspection(),
            CddlUnknownControlOperatorInspection(),
            CddlGenericArgumentsInspection(),
        )
        val problems = mutableListOf<String>()
        for (file in root.walkTopDown().filter { it.isFile && it.extension == "cddl" }.sorted()) {
            val text = file.readText().replace("\r\n", "\n")
            myFixture.configureByText(file.name, text)
            myFixture.doHighlighting()
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(myFixture.file)
            }
            val formatted = myFixture.editor.document.text
            if (tokens(formatted) != tokens(text)) {
                problems += "${file.relativeTo(root)}: reformatting changed tokens"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun tokens(text: String): List<String> {
        val lexer = CddlLexer()
        lexer.start(text)
        val result = mutableListOf<String>()
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) result += "${lexer.tokenType}:${lexer.tokenText}"
            lexer.advance()
        }
        return result
    }
}
