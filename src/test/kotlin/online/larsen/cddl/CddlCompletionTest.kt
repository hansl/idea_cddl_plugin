package online.larsen.cddl

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CddlCompletionTest : BasePlatformTestCase() {
    private fun complete(text: String): List<String> {
        myFixture.configureByText("test.cddl", text.trimIndent())
        myFixture.completeBasic()
        return myFixture.lookupElementStrings.orEmpty()
    }

    fun testLocalRulesAndPrelude() {
        val items = complete("root = [<caret>]\nperson = {name: tstr}")
        assertContainsElements(items, "person", "tstr", "uint", "bool", "root")
    }

    fun testPrefixWithDash() {
        val items = complete("root = [encoded-c<caret>]\nencoded-claims = int")
        assertContainsElements(items, "encoded-cbor", "encoded-claims")
        assertDoesntContain(items, "tstr")
    }

    fun testGenericParameters() {
        val items = complete("message<Body, Meta> = [<caret>]")
        assertContainsElements(items, "Body", "Meta")
    }

    fun testGenericRuleInsertsBrackets() {
        myFixture.configureByText("test.cddl", "root = mess<caret>\nmessage<T> = [T]")
        val items = myFixture.completeBasic()
        myFixture.lookup.currentItem = items.first { it.lookupString == "message" }
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
        val document = myFixture.editor.document.text
        val caret = myFixture.editor.caretModel.offset
        assertEquals("root = message<|>\nmessage<T> = [T]", document.substring(0, caret) + "|" + document.substring(caret))
    }

    fun testControlOperators() {
        val items = complete("root = tstr .<caret>")
        assertContainsElements(items, ".size", ".regexp", ".cbor", ".b64u-sloppy")
    }

    fun testControlOperatorWithPrefix() {
        myFixture.configureByText("test.cddl", "root = tstr .reg<caret>")
        myFixture.completeBasic()
        myFixture.checkResult("root = tstr .regexp<caret>")
    }

    fun testUndefinedNamesAtRuleName() {
        val items = complete("root = [person, address]\nperson = int\n<caret>")
        assertContainsElements(items, "address")
        assertDoesntContain(items, "person")
    }

    fun testImportedNames() {
        myFixture.addFileToProject("rfc9052.cddl", "COSE_Key = { * label => values }\nlabel = int / tstr\nvalues = any")
        val items = complete(";# import rfc9052 as cose\nroot = [cose.<caret>]")
        assertContainsElements(items, "cose.COSE_Key", "cose.label")
    }

    fun testProjectNames() {
        myFixture.addFileToProject("other.cddl", "shared-type = int\nshared-group = (a: int)")
        val items = complete("root = [shared<caret>]")
        assertContainsElements(items, "shared-type", "shared-group")
    }
}
