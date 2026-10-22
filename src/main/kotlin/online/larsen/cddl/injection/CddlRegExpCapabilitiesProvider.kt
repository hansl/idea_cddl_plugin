package online.larsen.cddl.injection

import com.intellij.psi.PsiElement
import online.larsen.cddl.psi.CddlControlType
import online.larsen.cddl.psi.operatorName
import org.intellij.lang.regexp.RegExpCapabilitiesProvider
import org.intellij.lang.regexp.RegExpCapability
import org.intellij.lang.regexp.RegExpCapability.*
import java.util.EnumSet

/**
 * Selects the regular expression dialect of injected fragments: `.regexp` uses XML Schema regular expressions
 * (RFC 8610, section 3.8.3), `.pcre` uses PCRE.
 */
class CddlRegExpCapabilitiesProvider : RegExpCapabilitiesProvider {
    override fun setup(host: PsiElement, defaultCapabilities: Set<RegExpCapability>): Set<RegExpCapability> =
        when ((host.parent as? CddlControlType)?.operatorName) {
            ".regexp" -> EnumSet.of(XML_SCHEMA_MODE, NESTED_CHARACTER_CLASSES, ALLOW_HORIZONTAL_WHITESPACE_CLASS, UNICODE_CATEGORY_SHORTHAND)
            ".pcre" -> EnumSet.of(
                POSIX_BRACKET_EXPRESSIONS, PCRE_BACK_REFERENCES, PCRE_NUMBERED_GROUP_REF, PCRE_CONDITIONS,
                ALLOW_HORIZONTAL_WHITESPACE_CLASS, EXTENDED_UNICODE_CHARACTER, UNICODE_CATEGORY_SHORTHAND,
                DANGLING_METACHARACTERS,
            )
            else -> defaultCapabilities
        }
}
