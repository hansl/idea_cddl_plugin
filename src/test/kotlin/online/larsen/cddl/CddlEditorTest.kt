package online.larsen.cddl

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import online.larsen.cddl.documentation.CddlDocumentationProvider
import online.larsen.cddl.inspections.CddlUnresolvedReferenceInspection
import online.larsen.cddl.inspections.CddlUnusedRuleInspection
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.structure.CddlStructureViewElement

class CddlEditorTest : BasePlatformTestCase() {
    private fun reformat(before: String, after: String) {
        myFixture.configureByText("test.cddl", before.trimIndent())
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResult(after.trimIndent())
    }

    fun testFormatSpacing() = reformat(
        """
        person={name :tstr ,? age:uint .le  150,* tstr=>any}
        message<T>=[id:uint,body:T]
        uri=#6.32 (tstr)
        a = b/c
        """,
        """
        person = {name: tstr, ? age: uint .le 150, * tstr => any}
        message<T> = [id: uint, body: T]
        uri = #6.32(tstr)
        a = b / c
        """,
    )

    fun testFormatIndentation() = reformat(
        """
        person = {
        name: tstr,
                ? address: {
        street: tstr
           }
        }
        delivery = (
        street: tstr //
        po-box: uint
        )
        choice = a /
        b /
        c
        """,
        """
        person = {
          name: tstr,
          ? address: {
            street: tstr
          }
        }
        delivery = (
          street: tstr //
          po-box: uint
        )
        choice = a /
            b /
            c
        """,
    )

    fun testFormatKeepsSignificantWhitespace() = reformat(
        "range = min .. max\nctl = tstr .size 3",
        "range = min .. max\nctl = tstr .size 3",
    )

    fun testFolding() {
        myFixture.testFolding("src/test/testData/folding/Folding.cddl")
    }

    fun testCreateRuleQuickFix() {
        myFixture.enableInspections(CddlUnresolvedReferenceInspection())
        myFixture.configureByText("test.cddl", "root = [per<caret>son] ; the root\nother = int")
        myFixture.launchAction(myFixture.findSingleIntention("Create rule 'person'"))
        myFixture.checkResult("root = [person] ; the root\nperson = <selection>any</selection>\nother = int")
    }

    fun testInsertSpaceBeforeControlOperatorQuickFix() {
        myFixture.enableInspections(CddlUnresolvedReferenceInspection())
        myFixture.configureByText("test.cddl", "root = tstr.si<caret>ze 3")
        myFixture.launchAction(myFixture.findSingleIntention("Insert space before control operator"))
        myFixture.checkResult("root = tstr .size 3")
    }

    fun testRemoveUnusedRuleQuickFix() {
        myFixture.enableInspections(CddlUnusedRuleInspection())
        myFixture.configureByText("test.cddl", "root = int\n; Not used\nun<caret>used = tstr\nlast = root")
        myFixture.launchAction(myFixture.findSingleIntention("Remove rule 'unused'"))
        myFixture.checkResult("root = int\nlast = root")
    }

    fun testDocumentationForRule() {
        myFixture.configureByText("test.cddl", "root = per<caret>son\n; A human being.\nperson = {name: tstr}")
        val target = myFixture.elementAtCaret
        val doc = CddlDocumentationProvider().generateDoc(target, null)!!
        assertTrue(doc, doc.contains("A human being."))
        assertTrue(doc, doc.contains("test.cddl"))
    }

    fun testDocumentationForPreludeType() {
        myFixture.configureByText("test.cddl", "root = ts<caret>tr")
        val doc = CddlDocumentationProvider().generateDoc(myFixture.elementAtCaret, null)!!
        assertTrue(doc, doc.contains("Text string (major type 3)."))
        assertTrue(doc, doc.contains("Standard prelude"))
    }

    fun testDocumentationForControlOperatorAndTag() {
        myFixture.configureByText("test.cddl", "root = tstr .si<caret>ze 3 / #6.32(tstr)")
        val provider = CddlDocumentationProvider()
        val operator = myFixture.file.findElementAt(myFixture.caretOffset)
        val operatorDoc = provider.generateDoc(provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, operator, 0), null)!!
        assertTrue(operatorDoc, operatorDoc.contains("Limits the size"))
        val tag = myFixture.file.findElementAt(myFixture.file.text.indexOf("#6.32"))
        val tagDoc = provider.generateDoc(provider.getCustomDocumentationElement(myFixture.editor, myFixture.file, tag, 0), null)!!
        assertTrue(tagDoc, tagDoc.contains("Tag 32: URI"))
    }

    private fun injectedAt(text: String, marker: String) = run {
        myFixture.configureByText("test.cddl", text)
        InjectedLanguageManager.getInstance(project).findInjectedElementAt(myFixture.file, text.indexOf(marker))
    }

    fun testRegexpInjection() {
        assertEquals("RegExp", injectedAt("root = tstr .regexp \"[a-z]+\"", "a-z")?.language?.id)
    }

    fun testRegexpDialects() {
        // XML Schema and PCRE do not nest character classes: "[" inside a class is a literal.
        myFixture.configureByText("test.cddl", "root = tstr .pcre \"[a-z[]+\" / tstr .regexp \"[\\\\p{L}-[a]]+\"")
        val errors = myFixture.doHighlighting().filter { it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR }
        assertEmpty(errors.map { it.description })
    }

    fun testNoInjectionOutsideRegexp() {
        assertNull(injectedAt("root = tstr .size \"[a-z]+\"", "a-z"))
    }

    fun testStructureView() {
        myFixture.configureByText("test.cddl", "person = {name: tstr, ? address: {street: tstr}}\ncity = (zip: uint)")
        val root = CddlStructureViewElement(myFixture.file as CddlFile)
        val rules = root.children.map { it.presentation.presentableText }
        assertEquals(listOf("person", "city"), rules)
        val person = root.children.first()
        assertEquals(listOf("name", "address"), person.children.map { it.presentation.presentableText })
        assertEquals(listOf("street"), person.children[1].children.map { it.presentation.presentableText })
    }

    fun testLineMarkers() {
        myFixture.configureByText("test.cddl", "attire = \"tie\"\nattire /= \"swimwear\"")
        myFixture.doHighlighting()
        val markers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
        assertEquals(2, markers.size)
    }

    fun testCommentLine() {
        myFixture.configureByText("test.cddl", "root<caret> = int")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("; root = int")
    }

    fun testBraceAutoClose() {
        myFixture.configureByText("test.cddl", "root = <caret>")
        myFixture.type("{")
        myFixture.checkResult("root = {<caret>}")
    }

    fun testEnterIndentsInsideBraces() {
        myFixture.configureByText("test.cddl", "person = {<caret>}")
        myFixture.type("\n")
        myFixture.checkResult("person = {\n  <caret>\n}")
    }

    fun testColorSettingsDemoTextParses() {
        val demo = online.larsen.cddl.highlighting.CddlColorSettingsPage().demoText.replace(Regex("</?[a-z]+>"), "")
        val file = myFixture.configureByText("demo.cddl", demo)
        val errors = com.intellij.psi.util.PsiTreeUtil.collectElementsOfType(file, com.intellij.psi.PsiErrorElement::class.java)
        assertEmpty(errors.map { it.errorDescription })
    }

    fun testGotoSymbol() {
        myFixture.addFileToProject("a.cddl", "person = {name: tstr}")
        myFixture.configureByText("b.cddl", "address = {street: tstr}")
        val names = mutableListOf<String>()
        online.larsen.cddl.navigation.CddlGotoSymbolContributor().processNames(
            { names += it; true },
            com.intellij.psi.search.GlobalSearchScope.projectScope(project),
            null,
        )
        assertContainsElements(names, "person", "address")
    }
}
