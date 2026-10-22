package online.larsen.cddl.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object CddlHighlighterColors {
    @JvmField val COMMENT = createTextAttributesKey("CDDL_COMMENT", Default.LINE_COMMENT)
    @JvmField val DIRECTIVE = createTextAttributesKey("CDDL_DIRECTIVE", Default.METADATA)
    @JvmField val NUMBER = createTextAttributesKey("CDDL_NUMBER", Default.NUMBER)
    @JvmField val TEXT_STRING = createTextAttributesKey("CDDL_TEXT_STRING", Default.STRING)
    @JvmField val BYTE_STRING = createTextAttributesKey("CDDL_BYTE_STRING", Default.STRING)
    @JvmField val VALID_ESCAPE = createTextAttributesKey("CDDL_VALID_ESCAPE", Default.VALID_STRING_ESCAPE)
    @JvmField val INVALID_ESCAPE = createTextAttributesKey("CDDL_INVALID_ESCAPE", Default.INVALID_STRING_ESCAPE)
    @JvmField val IDENTIFIER = createTextAttributesKey("CDDL_IDENTIFIER", Default.IDENTIFIER)

    @JvmField val TYPE_DECLARATION = createTextAttributesKey("CDDL_TYPE_DECLARATION", Default.FUNCTION_DECLARATION)
    @JvmField val GROUP_DECLARATION = createTextAttributesKey("CDDL_GROUP_DECLARATION", Default.FUNCTION_DECLARATION)
    @JvmField val TYPE_REFERENCE = createTextAttributesKey("CDDL_TYPE_REFERENCE", Default.CLASS_REFERENCE)
    @JvmField val GROUP_REFERENCE = createTextAttributesKey("CDDL_GROUP_REFERENCE", Default.INTERFACE_NAME)
    @JvmField val PRELUDE_TYPE = createTextAttributesKey("CDDL_PRELUDE_TYPE", Default.PREDEFINED_SYMBOL)
    @JvmField val GENERIC_PARAMETER = createTextAttributesKey("CDDL_GENERIC_PARAMETER", Default.PARAMETER)
    @JvmField val MEMBER_KEY = createTextAttributesKey("CDDL_MEMBER_KEY", Default.INSTANCE_FIELD)

    @JvmField val CONTROL_OPERATOR = createTextAttributesKey("CDDL_CONTROL_OPERATOR", Default.KEYWORD)
    @JvmField val TAG = createTextAttributesKey("CDDL_TAG", Default.METADATA)
    @JvmField val OCCURRENCE = createTextAttributesKey("CDDL_OCCURRENCE", Default.KEYWORD)
    @JvmField val OPERATOR = createTextAttributesKey("CDDL_OPERATOR", Default.OPERATION_SIGN)
    @JvmField val ASSIGNMENT = createTextAttributesKey("CDDL_ASSIGNMENT", Default.OPERATION_SIGN)
    @JvmField val COLON = createTextAttributesKey("CDDL_COLON", Default.OPERATION_SIGN)
    @JvmField val COMMA = createTextAttributesKey("CDDL_COMMA", Default.COMMA)
    @JvmField val PARENTHESES = createTextAttributesKey("CDDL_PARENTHESES", Default.PARENTHESES)
    @JvmField val BRACES = createTextAttributesKey("CDDL_BRACES", Default.BRACES)
    @JvmField val BRACKETS = createTextAttributesKey("CDDL_BRACKETS", Default.BRACKETS)
    @JvmField val ANGLE_BRACKETS = createTextAttributesKey("CDDL_ANGLE_BRACKETS", Default.BRACKETS)
    @JvmField val BAD_CHARACTER = createTextAttributesKey("CDDL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    val ALL: Array<TextAttributesKey> = arrayOf(
        COMMENT, DIRECTIVE, NUMBER, TEXT_STRING, BYTE_STRING, VALID_ESCAPE, INVALID_ESCAPE, IDENTIFIER,
        TYPE_DECLARATION, GROUP_DECLARATION, TYPE_REFERENCE, GROUP_REFERENCE, PRELUDE_TYPE, GENERIC_PARAMETER,
        MEMBER_KEY, CONTROL_OPERATOR, TAG, OCCURRENCE, OPERATOR, ASSIGNMENT, COLON, COMMA, PARENTHESES, BRACES,
        BRACKETS, ANGLE_BRACKETS, BAD_CHARACTER,
    )
}
