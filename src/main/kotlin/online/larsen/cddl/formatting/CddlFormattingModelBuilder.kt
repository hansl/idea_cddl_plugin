package online.larsen.cddl.formatting

import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import online.larsen.cddl.CddlLanguage
import online.larsen.cddl.psi.CddlTokenSets
import online.larsen.cddl.psi.CddlTypes.*

class CddlFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val root = CddlBlock(formattingContext.node, Indent.getNoneIndent(), spacingBuilder(settings))
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.containingFile, root, settings)
    }

    /**
     * Spacing rules. Pairs without a rule keep their current spacing: CDDL is sensitive to some whitespace (a name
     * directly followed by `.size` or `..` would merge into a single name), so only safe normalizations are applied.
     */
    private fun spacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        val keyNodes = TokenSet.create(BAREWORD_MEMBER_KEY, VALUE_MEMBER_KEY, TYPE_MEMBER_KEY)
        return SpacingBuilder(settings, CddlLanguage)
            .around(CddlTokenSets.ASSIGNMENTS).spaces(1)
            .between(ID, GENERIC_PARAMS).spaces(0)
            .between(NAME_REF, GENERIC_ARGS).spaces(0)
            .after(LT).spaces(0)
            .before(GT).spaces(0)
            .before(COMMA).spaces(0)
            .after(COMMA).spaces(1)
            .before(COLON).spaces(0)
            .before(ARROW).spaces(1)
            .before(CARET).spaces(1)
            .afterInside(keyNodes, GROUP_ENTRY).spaces(1)
            .afterInside(OCCURRENCE, GROUP_ENTRY).spaces(1)
            .aroundInside(STAR, OCCURRENCE).spaces(0)
            .around(TCHOICE).spaces(1)
            .around(GCHOICE).spaces(1)
            .around(CTLOP).spaces(1)
            .between(TAG_HEAD, LPAREN).spaces(0)
            .between(TAG_HEAD, DOT).spaces(0)
            .between(MAJOR_TYPE_HEAD, DOT).spaces(0)
            .between(DOT, LT).spaces(0)
            .before(GENERIC_ARGS).spaces(0)
    }
}

class CddlBlock(
    node: ASTNode,
    private val indent: Indent,
    private val spacingBuilder: SpacingBuilder,
) : AbstractBlock(node, null, null) {

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.textLength > 0) {
                blocks += CddlBlock(child, childIndent(child), spacingBuilder)
            }
            child = child.treeNext
        }
        return blocks
    }

    private fun childIndent(child: ASTNode): Indent {
        val parentType = myNode.elementType
        val childType = child.elementType
        return when {
            parentType in BRACKETED && childType !in BRACKETS -> Indent.getNormalIndent()
            parentType == CHOICE_TYPE -> Indent.getContinuationWithoutFirstIndent()
            parentType == RULE && childType in VALUE_CONTINUATION -> Indent.getContinuationWithoutFirstIndent()
            parentType == GENERIC_ARGS || parentType == GENERIC_PARAMS -> Indent.getContinuationWithoutFirstIndent()
            else -> Indent.getNoneIndent()
        }
    }

    override fun getIndent(): Indent = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = when (myNode.elementType) {
        in BRACKETED -> ChildAttributes(Indent.getNormalIndent(), null)
        CHOICE_TYPE -> ChildAttributes(Indent.getContinuationIndent(), null)
        else -> ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    private companion object {
        val BRACKETED = TokenSet.create(MAP_TYPE, ARRAY_TYPE, PAREN_TYPE, ENUM_TYPE, TAG_TYPE)
        val BRACKETS = TokenSet.create(LBRACE, RBRACE, LBRACKET, RBRACKET, LPAREN, RPAREN, AMPERSAND, TAG_HEAD, DOT, LT, GT)

        /** Rule values that continue on the next lines get a continuation indent (but bracketed values do not). */
        val VALUE_CONTINUATION = TokenSet.create(
            CHOICE_TYPE, RANGE_TYPE, CONTROL_TYPE, GROUP_ENTRY, REF_TYPE, LITERAL_TYPE, UNWRAP_TYPE, MAJOR_TYPE,
            ANY_TYPE,
        )
    }
}
