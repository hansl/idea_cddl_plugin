package online.larsen.cddl

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import online.larsen.cddl.inspections.CddlDuplicateRuleInspection
import online.larsen.cddl.inspections.CddlGenericArgumentsInspection
import online.larsen.cddl.inspections.CddlUnknownControlOperatorInspection
import online.larsen.cddl.inspections.CddlUnresolvedReferenceInspection
import online.larsen.cddl.inspections.CddlUnusedRuleInspection

class CddlInspectionsTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(
            CddlUnresolvedReferenceInspection(),
            CddlUnusedRuleInspection(),
            CddlDuplicateRuleInspection(),
            CddlUnknownControlOperatorInspection(),
            CddlGenericArgumentsInspection(),
        )
    }

    private fun check(text: String) {
        myFixture.configureByText("test.cddl", text.trimIndent())
        myFixture.checkHighlighting(true, false, true)
    }

    fun testValidSpecificationHasNoProblems() = check(
        """
        root = [person, text-message, color, tagged]
        person = {
          name: tstr .size (1..64),
          ? age: uint,
          contact,
          * tstr => any
        }
        contact = (email: tstr // phone: tstr)
        message<T> = [id: uint, body: T]
        text-message = message<tstr>
        color = &colors
        colors = (red: 0, green: 1)
        ${'$'}extension-point /= int
        tagged = #6.32(tstr) / #7.25 / #
        """,
    )

    fun testUnresolvedReference() = check(
        """
        root = [known, <error descr="Unresolved reference 'unknown'">unknown</error>]
        known = int
        """,
    )

    fun testUnusedRule() = check(
        """
        root = [used]
        used = int
        <warning descr="Rule 'unused' is never used">unused</warning> = tstr
        """,
    )

    fun testDuplicateRule() = check(
        """
        root = [a, b]
        a = int
        <error descr="'a' is already defined; use '/=' or '//=' to add choices">a</error> = tstr
        a /= bool
        b = (x: int)
        b //= (y: int)
        """,
    )

    fun testInconsistentExtensions() = check(
        """
        root = [a, b]
        a = int
        a <error descr="'a' is a type: add type choices with '/='">//=</error> (x: int)
        b = (x: int)
        b <error descr="'b' is a group: add group choices with '//='">/=</error> tstr
        """,
    )

    fun testUnknownControlOperator() = check(
        """
        root = tstr <warning descr="Unknown control operator '.sise'">.sise</warning> 3 / tstr .size 3
        """,
    )

    fun testGenericArguments() = check(
        """
        root = [pair<int, tstr>, pair<warning descr="'pair' expects 2 generic argument(s), but 1 given"><int></warning>, plain<warning descr="'plain' does not take generic arguments"><int></warning>]
        pair<A, B> = [A, B]
        plain = int
        """.replace("<warning", "<error").replace("</warning>", "</error>"),
    )

    fun testInvalidEscape() = check(
        """
        root = tstr .regexp "<warning descr="Invalid escape sequence (RFC 9682 only allows JSON-style escapes)">\d</warning>+"
        """,
    )

    fun testDirectiveWithMissingModuleDoesNotReportNames() = check(
        """
        ;# import rfc9052 as cose
        root = [cose.COSE_Key, <error descr="Unresolved reference 'other'">other</error>]
        """,
    )

    fun testUndefinedSocketsAreNotReported() = check(
        """
        root = { ? name: tstr, * ${'$'}${'$'}root-extension, other: ${'$'}other-socket }
        """,
    )

    fun testIdenticalRedefinitionIsWeakWarning() = check(
        """
        root = [ttl]
        ttl = 0..4294967295 ; in milliseconds
        <weak_warning descr="'ttl' is defined again with the same value">ttl</weak_warning> = 0..4294967295
        """,
    )
}
