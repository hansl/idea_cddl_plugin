package online.larsen.cddl.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.CddlIcons
import javax.swing.Icon

class CddlColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = CddlIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = CddlSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAGS

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "CDDL"

    private companion object {
        val DESCRIPTORS: Array<AttributesDescriptor> = with(CddlHighlighterColors) {
            arrayOf(
                descriptor("color.settings.comment", COMMENT),
                descriptor("color.settings.directive", DIRECTIVE),
                descriptor("color.settings.number", NUMBER),
                descriptor("color.settings.text.string", TEXT_STRING),
                descriptor("color.settings.byte.string", BYTE_STRING),
                descriptor("color.settings.valid.escape", VALID_ESCAPE),
                descriptor("color.settings.invalid.escape", INVALID_ESCAPE),
                descriptor("color.settings.identifier", IDENTIFIER),
                descriptor("color.settings.type.declaration", TYPE_DECLARATION),
                descriptor("color.settings.group.declaration", GROUP_DECLARATION),
                descriptor("color.settings.type.reference", TYPE_REFERENCE),
                descriptor("color.settings.group.reference", GROUP_REFERENCE),
                descriptor("color.settings.prelude.type", PRELUDE_TYPE),
                descriptor("color.settings.generic.parameter", GENERIC_PARAMETER),
                descriptor("color.settings.member.key", MEMBER_KEY),
                descriptor("color.settings.control.operator", CONTROL_OPERATOR),
                descriptor("color.settings.tag", TAG),
                descriptor("color.settings.occurrence", OCCURRENCE),
                descriptor("color.settings.operator", OPERATOR),
                descriptor("color.settings.assignment", ASSIGNMENT),
                descriptor("color.settings.colon", COLON),
                descriptor("color.settings.comma", COMMA),
                descriptor("color.settings.parentheses", PARENTHESES),
                descriptor("color.settings.braces", BRACES),
                descriptor("color.settings.brackets", BRACKETS),
                descriptor("color.settings.angle.brackets", ANGLE_BRACKETS),
                descriptor("color.settings.bad.character", BAD_CHARACTER),
            )
        }

        val TAGS: Map<String, TextAttributesKey> = with(CddlHighlighterColors) {
            mapOf(
                "type" to TYPE_DECLARATION,
                "group" to GROUP_DECLARATION,
                "tref" to TYPE_REFERENCE,
                "gref" to GROUP_REFERENCE,
                "prelude" to PRELUDE_TYPE,
                "param" to GENERIC_PARAMETER,
                "key" to MEMBER_KEY,
                "esc" to VALID_ESCAPE,
                "badesc" to INVALID_ESCAPE,
            )
        }

        fun descriptor(key: String, attributes: TextAttributesKey) =
            AttributesDescriptor(CddlBundle.message(key), attributes)

        val DEMO_TEXT = """
            ;# import rfc9052 as cose
            ; A person, as described in RFC 8610
            <type>person</type> = {
              <key>name</key>: <prelude>tstr</prelude> .size (1..64),
              ? <key>age</key>: <prelude>uint</prelude> .le 150,
              <gref>contact</gref>,
              * <prelude>tstr</prelude> => <prelude>any</prelude>
            }

            <group>contact</group> = (
              <key>email</key>: <prelude>tstr</prelude> .regexp "[^@]+@[^@]+<esc>\\</esc>.[a-z]+" //
              <key>phone</key>: <prelude>tstr</prelude>
            )

            <type>tagged-uri</type> = #6.32(<prelude>tstr</prelude>) / #7.25 / h'0102 03'
            <type>message</type><<param>T</param>> = [<key>id</key>: <prelude>uint</prelude>, <key>body</key>: <param>T</param>]
            <type>text-message</type> = <tref>message</tref><<prelude>tstr</prelude>>
            <type>colors</type> = &(<key>red</key>: 0, <key>green</key>: 1, <key>blue</key>: 0x10)
            <type>colors</type> /= "<badesc>\d</badesc>"
        """.trimIndent()
    }
}
