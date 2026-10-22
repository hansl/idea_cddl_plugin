package online.larsen.cddl.parser

import com.intellij.testFramework.ParsingTestCase

class CddlParsingTest : ParsingTestCase("parser", "cddl", CddlParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    override fun includeRanges(): Boolean = true

    fun testRules() = doTest(true, true)

    fun testTypes() = doTest(true, true)

    fun testGroups() = doTest(true, true)

    fun testComments() = doTest(true, true)

    fun testErrorRecovery() = doTest(true, false)

    fun testCose() = doTest(true, true)
}
