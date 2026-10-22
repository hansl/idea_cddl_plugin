package online.larsen.cddl.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlGenericParam
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.containingRule
import online.larsen.cddl.psi.genericParamList
import online.larsen.cddl.psi.referenceName
import online.larsen.cddl.psi.resolveTargets

/**
 * Name resolution. A name is looked up, in order, among:
 *  1. the generic parameters of the enclosing rule;
 *  2. the rules of the same file (all of them, when a name is extended with `/=` or `//=`);
 *  3. the rules made visible by `;# import` / `;# include` directives;
 *  4. the standard prelude;
 *  5. the rules of the other CDDL files of the project, for files that do not use directives (specifications are
 *     often split into several files that are concatenated by tooling).
 */
object CddlResolver {
    fun resolve(ref: CddlNameRef): List<PsiElement> {
        val name = ref.referenceName
        resolveGenericParam(ref, name)?.let { return listOf(it) }
        val file = ref.containingFile as? CddlFile ?: return emptyList()
        return resolveRule(file, name)
    }

    fun resolveGenericParam(element: PsiElement, name: String): CddlGenericParam? =
        element.containingRule?.genericParamList?.firstOrNull { it.name == name }

    fun resolveRule(file: CddlFile, name: String): List<CddlRule> {
        file.ruleMap[name]?.let { return it }

        val usesDirectives = CddlDirectives.directives(file).isNotEmpty()
        if (usesDirectives) {
            CddlDirectives.resolve(file, name).ifNotEmpty { return it }
        }

        CddlPrelude.getInstance(file.project).rules(name).ifNotEmpty { return it }

        if (!usesDirectives && !CddlPrelude.isPrelude(file)) {
            val original = file.originalFile
            return CddlRuleNameIndex.findRules(file.project, name, GlobalSearchScope.projectScope(file.project))
                .filter { it.containingFile != original }
        }
        return emptyList()
    }

    /** True when [ref] resolves to the standard prelude. */
    fun isPreludeReference(ref: CddlNameRef): Boolean =
        ref.resolveTargets.any(CddlPrelude::isPrelude)

    private inline fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
        if (isNotEmpty()) block(this)
    }
}
