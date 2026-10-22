package online.larsen.cddl.parser

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import java.io.File

/**
 * Parses every `.cddl` file below the directory named by the `CDDL_CORPUS` environment variable and fails if any of
 * them has a syntax error. Does nothing when the variable is not set.
 *
 * ```
 * CDDL_CORPUS=/path/to/specs ./gradlew test --tests '*CddlCorpusTest*'
 * ```
 */
class CddlCorpusTest : ParsingTestCase("", "cddl", CddlParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testCorpus() {
        val root = System.getenv("CDDL_CORPUS")?.let(::File) ?: return
        val failures = root.walkTopDown().filter { it.isFile && it.extension == "cddl" }.sorted().mapNotNull { file ->
            val text = file.readText().replace("\r\n", "\n")
            val psi = createPsiFile(file.name, text)
            val errors = PsiTreeUtil.collectElementsOfType(psi, PsiErrorElement::class.java)
            if (errors.isEmpty()) return@mapNotNull null
            errors.joinToString("\n", "${file.relativeTo(root)}:\n") { error ->
                val offset = error.textRange.startOffset
                val line = text.substring(0, offset).count { it == '\n' } + 1
                val lineText = text.lineSequence().drop(line - 1).first()
                "  line $line: ${error.errorDescription}\n    $lineText"
            }
        }.toList()
        assertTrue("Files with syntax errors:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
