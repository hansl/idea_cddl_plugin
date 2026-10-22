package online.larsen.cddl.psi

import com.intellij.psi.tree.TokenSet
import online.larsen.cddl.psi.CddlTypes.*

object CddlTokenSets {
    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(COMMENT, DIRECTIVE)

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(TEXT_STRING, BYTE_STRING)

    @JvmField
    val LITERALS: TokenSet = TokenSet.create(NUMBER, TEXT_STRING, BYTE_STRING)

    @JvmField
    val ASSIGNMENTS: TokenSet = TokenSet.create(ASSIGN, TCHOICE_ASSIGN, GCHOICE_ASSIGN)

    @JvmField
    val RANGE_OPERATORS: TokenSet = TokenSet.create(INCLUSIVE_RANGE, EXCLUSIVE_RANGE)

    @JvmField
    val OCCURRENCE_INDICATORS: TokenSet = TokenSet.create(QUESTION, STAR, PLUS)

    @JvmField
    val MAJOR_TYPES: TokenSet = TokenSet.create(TAG_HEAD, MAJOR_TYPE_HEAD, HASH)
}
