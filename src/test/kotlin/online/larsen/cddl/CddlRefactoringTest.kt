package online.larsen.cddl

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CddlRefactoringTest : BasePlatformTestCase() {
    fun testRenameRuleFromDefinition() {
        myFixture.configureByText("test.cddl", "root = [person, [* person]]\nper<caret>son = {name: tstr}")
        myFixture.renameElementAtCaret("human")
        myFixture.checkResult("root = [human, [* human]]\nhuman = {name: tstr}")
    }

    fun testRenameRuleFromReferenceRenamesExtensions() {
        myFixture.configureByText("test.cddl", "root = att<caret>ire\nattire = \"tie\"\nattire /= \"swimwear\"")
        myFixture.renameElementAtCaret("outfit")
        myFixture.checkResult("root = outfit\noutfit = \"tie\"\noutfit /= \"swimwear\"")
    }

    fun testRenameGenericParameter() {
        myFixture.configureByText("test.cddl", "message<T> = [id: uint, body: <caret>T]\nT = int\nroot = T")
        myFixture.renameElementAtCaret("Body")
        myFixture.checkResult("message<Body> = [id: uint, body: Body]\nT = int\nroot = T")
    }

    fun testRenameAcrossFiles() {
        myFixture.addFileToProject("other.cddl", "uses = [shared]")
        myFixture.configureByText("test.cddl", "sha<caret>red = int")
        myFixture.renameElementAtCaret("common")
        myFixture.checkResult("other.cddl", "uses = [common]", false)
    }

    fun testRenamePrefixedImport() {
        myFixture.addFileToProject("main.cddl", ";# import lib as l\nroot = [l.label]")
        myFixture.configureByText("lib.cddl", "la<caret>bel = int")
        myFixture.renameElementAtCaret("tag")
        myFixture.checkResult("main.cddl", ";# import lib as l\nroot = [l.tag]", false)
    }

    fun testFindUsages() {
        myFixture.configureByText("test.cddl", "root = [person, {p: person}]\nper<caret>son = int\nother = person")
        val usages = myFixture.findUsages(myFixture.elementAtCaret)
        assertEquals(3, usages.size)
    }
}
