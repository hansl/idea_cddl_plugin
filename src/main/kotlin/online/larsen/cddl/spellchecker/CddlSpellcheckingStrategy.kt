package online.larsen.cddl.spellchecker

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.IdentifierSplitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.intellij.spellchecker.tokenizer.TokenizerBase
import online.larsen.cddl.psi.CddlBarewordMemberKey
import online.larsen.cddl.psi.CddlTypes

/** Spell checks comments, rule and parameter names, and member keys (but not module directives). */
class CddlSpellcheckingStrategy : SpellcheckingStrategy() {
    override fun getTokenizer(element: PsiElement): Tokenizer<*> = when {
        element is PsiComment && element.node.elementType == CddlTypes.DIRECTIVE -> EMPTY_TOKENIZER
        element.node?.elementType == CddlTypes.ID && element.parent is CddlBarewordMemberKey -> IDENTIFIER_TOKENIZER
        else -> super.getTokenizer(element)
    }

    private companion object {
        val IDENTIFIER_TOKENIZER = TokenizerBase<PsiElement>(IdentifierSplitter.getInstance())
    }
}
