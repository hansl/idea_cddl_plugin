package online.larsen.cddl.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.CddlControlOperators
import online.larsen.cddl.CddlLanguage
import online.larsen.cddl.psi.CddlDocUtil
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypes
import online.larsen.cddl.psi.containingRule
import online.larsen.cddl.resolve.CddlPrelude
import online.larsen.cddl.resolve.CddlResolver

class CddlDocumentationProvider : AbstractDocumentationProvider() {
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? = contextElement?.takeIf { it.node?.elementType in SELF_DOCUMENTED_TOKENS }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? = when {
        element is CddlRule -> ruleDoc(element)
        element is CddlGenericParam -> genericParamDoc(element)
        element?.node?.elementType == CddlTypes.CTLOP -> controlOperatorDoc(element.text)
        element?.node?.elementType in MAJOR_TYPE_TOKENS -> majorTypeDoc(element!!.text)
        else -> null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? = when (element) {
        is CddlRule -> StringUtil.escapeXmlEntities(firstLine(element.text)) + location(element)
        is CddlGenericParam -> StringUtil.escapeXmlEntities(
            CddlBundle.message("doc.generic.parameter.of", element.name ?: "", ruleName(element)),
        )
        else -> null
    }

    private fun ruleDoc(rule: CddlRule): String {
        val definitions = (rule.containingFile as? CddlFile)?.let { file ->
            rule.name?.let { CddlResolver.resolveRule(file, it) }?.filter { it.containingFile == file }
        }.orEmpty().ifEmpty { listOf(rule) }

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            for ((index, definition) in definitions.withIndex()) {
                if (index > 0) append("\n")
                append(highlight(rule, abbreviate(definition.text)))
            }
            append(DocumentationMarkup.DEFINITION_END)
            val comments = definitions.mapNotNull(CddlDocUtil::docComment)
            if (comments.isNotEmpty()) {
                append(DocumentationMarkup.CONTENT_START)
                append(comments.joinToString("<p>") { StringUtil.escapeXmlEntities(it).replace("\n", "<br>") })
                append(DocumentationMarkup.CONTENT_END)
            }
            append(DocumentationMarkup.SECTIONS_START)
            section(CddlBundle.message("doc.section.defined.in"), StringUtil.escapeXmlEntities(fileName(rule)))
            append(DocumentationMarkup.SECTIONS_END)
        }
    }

    private fun genericParamDoc(param: CddlGenericParam): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append(StringUtil.escapeXmlEntities(param.name ?: ""))
        append(DocumentationMarkup.DEFINITION_END)
        append(DocumentationMarkup.CONTENT_START)
        append(StringUtil.escapeXmlEntities(CddlBundle.message("doc.generic.parameter.description", ruleName(param))))
        append(DocumentationMarkup.CONTENT_END)
    }

    private fun controlOperatorDoc(name: String): String {
        val operator = CddlControlOperators[name]
            ?: return DocumentationMarkup.CONTENT_START + CddlBundle.message("doc.unknown.control.operator", name) +
                DocumentationMarkup.CONTENT_END
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(StringUtil.escapeXmlEntities(operator.signature))
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(StringUtil.escapeXmlEntities(operator.description))
            append(DocumentationMarkup.CONTENT_END)
            append(DocumentationMarkup.SECTIONS_START)
            section(CddlBundle.message("doc.section.defined.in"), StringUtil.escapeXmlEntities(operator.reference))
            append(DocumentationMarkup.SECTIONS_END)
        }
    }

    private fun majorTypeDoc(text: String): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append(StringUtil.escapeXmlEntities(text))
        append(DocumentationMarkup.DEFINITION_END)
        append(DocumentationMarkup.CONTENT_START)
        append(StringUtil.escapeXmlEntities(CddlMajorTypes.describe(text)))
        append(DocumentationMarkup.CONTENT_END)
    }

    private fun StringBuilder.section(header: String, content: String) {
        append(DocumentationMarkup.SECTION_HEADER_START).append(header)
        append(DocumentationMarkup.SECTION_SEPARATOR).append(content)
        append(DocumentationMarkup.SECTION_END)
    }

    private fun highlight(context: PsiElement, code: String): String =
        QuickDocHighlightingHelper.getStyledCodeFragment(context.project, CddlLanguage, code)

    private fun abbreviate(text: String): String {
        val lines = text.lines()
        return if (lines.size > MAX_DEFINITION_LINES) {
            lines.take(MAX_DEFINITION_LINES).joinToString("\n") + "\n  ..."
        } else {
            text
        }
    }

    private fun firstLine(text: String): String {
        val line = text.lineSequence().first()
        return if (line.length < text.length) "$line ..." else line
    }

    private fun location(rule: CddlRule): String = " [${StringUtil.escapeXmlEntities(fileName(rule))}]"

    private fun fileName(rule: CddlRule): String =
        if (CddlPrelude.isPrelude(rule)) CddlBundle.message("doc.prelude") else rule.containingFile?.name ?: ""

    private fun ruleName(element: PsiElement): String = element.containingRule?.name ?: ""

    private companion object {
        const val MAX_DEFINITION_LINES = 30
        val MAJOR_TYPE_TOKENS = setOf(CddlTypes.TAG_HEAD, CddlTypes.MAJOR_TYPE_HEAD, CddlTypes.HASH)
        val SELF_DOCUMENTED_TOKENS = MAJOR_TYPE_TOKENS + CddlTypes.CTLOP
    }
}

/** Describes `#`, `#N` and `#N.M` (major types, tags and simple values). */
object CddlMajorTypes {
    private val MAJOR_TYPES = listOf(
        "unsigned integer", "negative integer", "byte string", "text string", "array", "map", "tagged data item",
        "simple value or floating-point number",
    )

    private val TAGS = mapOf(
        0L to "Standard date/time string (RFC 3339)",
        1L to "Epoch-based date/time",
        2L to "Unsigned bignum",
        3L to "Negative bignum",
        4L to "Decimal fraction",
        5L to "Bigfloat",
        16L to "COSE_Encrypt0 (RFC 9052)",
        17L to "COSE_Mac0 (RFC 9052)",
        18L to "COSE_Sign1 (RFC 9052)",
        21L to "Expected conversion to base64url encoding",
        22L to "Expected conversion to base64 encoding",
        23L to "Expected conversion to base16 encoding",
        24L to "Encoded CBOR data item",
        32L to "URI (RFC 3986)",
        33L to "base64url-encoded text",
        34L to "base64-encoded text",
        35L to "Regular expression",
        36L to "MIME message",
        37L to "Binary UUID (RFC 4122)",
        61L to "CBOR Web Token (RFC 8392)",
        96L to "COSE_Encrypt (RFC 9052)",
        97L to "COSE_Mac (RFC 9052)",
        98L to "COSE_Sign (RFC 9052)",
        100L to "Number of days since the epoch date 1970-01-01 (RFC 8943)",
        1004L to "RFC 3339 full-date string (RFC 8943)",
        55799L to "Self-described CBOR",
    )

    private val SIMPLE_VALUES = mapOf(
        20L to "false",
        21L to "true",
        22L to "null",
        23L to "undefined",
        25L to "half-precision (16-bit) floating-point number",
        26L to "single-precision (32-bit) floating-point number",
        27L to "double-precision (64-bit) floating-point number",
    )

    fun describe(text: String): String {
        if (text == "#") return "Any data item."
        val major = text.getOrNull(1)?.digitToIntOrNull() ?: return text
        val argument = text.substringAfter('.', "").takeIf { it.isNotEmpty() }?.let(::parseUint)
        val kind = MAJOR_TYPES.getOrNull(major) ?: return "Major type $major (not defined by CBOR)."
        return when {
            argument == null -> "Major type $major: any $kind."
            major == 6 -> "Tag $argument" + (TAGS[argument]?.let { ": $it." } ?: ".")
            major == 7 -> "Simple value $argument" + (SIMPLE_VALUES[argument]?.let { ": $it." } ?: ".")
            else -> "Major type $major ($kind) with additional information $argument."
        }
    }

    private fun parseUint(text: String): Long? = when {
        text.startsWith("0x", ignoreCase = true) -> text.substring(2).toLongOrNull(16)
        text.startsWith("0b", ignoreCase = true) -> text.substring(2).toLongOrNull(2)
        else -> text.toLongOrNull()
    }
}
