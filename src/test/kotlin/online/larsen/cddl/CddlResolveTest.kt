package online.larsen.cddl

import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlReference

class CddlResolveTest : BasePlatformTestCase() {
    private fun reference(text: String, fileName: String = "test.cddl"): CddlReference {
        myFixture.configureByText(fileName, text.trimIndent())
        return myFixture.getReferenceAtCaretPosition() as CddlReference
    }

    private fun targets(ref: CddlReference) = ref.multiResolve(false).map { it.element }

    fun testLocalRule() {
        val target = reference("root = [fo<caret>o]\nfoo = int").resolve()
        assertInstanceOf(target, CddlRule::class.java)
        assertEquals("foo", (target as CddlRule).name)
    }

    fun testNameWithDashesAndDots() {
        val target = reference("root = [my-name.v<caret>1]\nmy-name.v1 = int").resolve()
        assertEquals("my-name.v1", (target as CddlRule).name)
    }

    fun testPreludeType() {
        val target = reference("root = ts<caret>tr").resolve()
        assertNotNull(target)
        assertTrue(CddlPrelude.isPrelude(target!!))
        assertEquals("tstr", (target as PsiNamedElement).name)
    }

    fun testLocalRuleShadowsPrelude() {
        val target = reference("root = ui<caret>nt\nuint = 0..255").resolve()
        assertFalse(CddlPrelude.isPrelude(target!!))
    }

    fun testGenericParameter() {
        val target = reference("message<T> = [body: <caret>T]\nT = int").resolve()
        assertInstanceOf(target, CddlGenericParam::class.java)
    }

    fun testAllDefinitionsOfExtendedRule() {
        val ref = reference(
            """
            root = [att<caret>ire]
            attire /= "swimwear"
            attire = "bow tie" / "necktie"
            """,
        )
        assertEquals(2, targets(ref).size)
        assertEquals(CddlAssignment.DEFINE, (ref.resolve() as CddlRule).assignment)
    }

    fun testUnresolved() {
        assertEmpty(targets(reference("root = [unkn<caret>own]")))
    }

    fun testOtherFileOfProject() {
        myFixture.addFileToProject("types.cddl", "shared = int")
        val target = reference("root = [sha<caret>red]").resolve()
        assertEquals("types.cddl", target?.containingFile?.name)
    }

    fun testImportDirective() {
        myFixture.addFileToProject("rfc9052.cddl", "COSE_Key = { * label => values }\nlabel = int / tstr\nvalues = any")
        val target = reference(";# import rfc9052\nroot = COSE<caret>_Key").resolve()
        assertEquals("rfc9052.cddl", target?.containingFile?.name)
    }

    fun testImportDirectiveWithPrefix() {
        myFixture.addFileToProject("rfc9052.cddl", "COSE_Key = { * label => values }\nlabel = int / tstr\nvalues = any")
        val target = reference(";# import rfc9052 as cose\nroot = cose.la<caret>bel").resolve()
        assertEquals("label", (target as CddlRule).name)
        assertEquals("rfc9052.cddl", target.containingFile.name)
    }

    fun testPrefixedImportHidesUnprefixedNames() {
        myFixture.addFileToProject("rfc9052.cddl", "label = int / tstr")
        assertEmpty(targets(reference(";# import rfc9052 as cose\nroot = la<caret>bel")))
    }

    fun testIncludeOnlySelectedNames() {
        myFixture.addFileToProject("lib.cddl", "a = int\nb = tstr")
        assertNotNull(reference(";# include a from lib\nroot = [<caret>a]").resolve())
        assertEmpty(targets(reference(";# include a from lib\nroot = [<caret>b]", "other.cddl")))
    }

    fun testFilesWithDirectivesDoNotUseProjectWideNames() {
        myFixture.addFileToProject("lib.cddl", "a = int")
        myFixture.addFileToProject("unrelated.cddl", "b = int")
        assertEmpty(targets(reference(";# import lib\nroot = [<caret>b]")))
    }

    fun testModuleReferenceInDirective() {
        myFixture.addFileToProject("rfc9052.cddl", "label = int")
        myFixture.configureByText("test.cddl", ";# import rfc90<caret>52 as cose\nroot = cose.label")
        val target = myFixture.getReferenceAtCaretPosition()?.resolve()
        assertEquals("rfc9052.cddl", (target as? com.intellij.psi.PsiFile)?.name)
    }
}
