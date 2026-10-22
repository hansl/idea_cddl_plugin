package online.larsen.cddl.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypes

/**
 * A module structure directive (draft-ietf-cbor-cddl-modules), written as a comment:
 * ```
 * ;# import rfc9052 as cose
 * ;# include label, values from rfc9052
 * ```
 */
data class CddlDirective(
    val kind: Kind,
    /** The names of the `from` clause, or `null` when there is none. */
    val names: List<String>?,
    val module: String,
    /** The namespace prefix of the `as` clause, without the trailing dot. */
    val alias: String?,
    /** The range of [module] in the directive's text. */
    val moduleRange: TextRange,
) {
    enum class Kind(val keyword: String) { IMPORT("import"), INCLUDE("include") }

    private val prefix: String? get() = alias?.let { "$it." }

    /** An import pulls in every rule that is referenced; an include only the selected ones (or all when none are). */
    private val selectsAll: Boolean get() = names == null || "*" in names || kind == Kind.IMPORT

    /** Maps a name as seen by the importing file to the names of the module's rules it can denote. */
    fun moduleNamesFor(visibleName: String): List<String> {
        val prefix = prefix
        val result = mutableListOf<String>()
        if (selectsAll) {
            when {
                prefix == null -> result += visibleName
                visibleName.startsWith(prefix) -> result += visibleName.removePrefix(prefix)
            }
        }
        for (entry in names.orEmpty()) {
            if (entry == "*") continue
            if (prefix != null && entry.startsWith(prefix)) {
                if (visibleName == entry) result += entry.removePrefix(prefix)
            } else {
                // An unprefixed name in the `from` clause is also made available under its own name.
                if (visibleName == entry) result += entry
                if (prefix != null && visibleName == prefix + entry) result += entry
            }
        }
        return result.distinct()
    }

    /** Maps the names of a module's rules to the names under which they are visible in the importing file. */
    fun visibleNamesFor(moduleNames: Collection<String>): List<String> {
        val prefix = prefix.orEmpty()
        val result = mutableListOf<String>()
        if (selectsAll) moduleNames.mapTo(result) { prefix + it }
        for (entry in names.orEmpty()) {
            if (entry == "*") continue
            result += entry
            if (prefix.isNotEmpty() && !entry.startsWith(prefix)) result += prefix + entry
        }
        return result.distinct()
    }
}

object CddlDirectives {
    private val DIRECTIVE = Regex(
        """^;#\s+(import|include)\s+(?:(.+?)\s+from\s+)?([-.\w]+)(?:\s+as\s+([$@A-Za-z_][$@\w]*))?\s*$""",
    )

    fun parse(comment: PsiComment): CddlDirective? {
        if (comment.node.elementType != CddlTypes.DIRECTIVE) return null
        return parse(comment.text)
    }

    fun parse(text: String): CddlDirective? {
        val match = DIRECTIVE.find(text) ?: return null
        val kind = if (match.groupValues[1] == "import") CddlDirective.Kind.IMPORT else CddlDirective.Kind.INCLUDE
        val names = match.groups[2]?.value?.split(',', ' ', '\t')?.filter { it.isNotEmpty() }
        val module = match.groups[3]!!
        return CddlDirective(
            kind = kind,
            names = names,
            module = module.value,
            alias = match.groups[4]?.value,
            moduleRange = TextRange(module.range.first, module.range.last + 1),
        )
    }

    fun directives(file: CddlFile): List<CddlDirective> = CachedValuesManager.getCachedValue(file) {
        val directives = PsiTreeUtil.getChildrenOfTypeAsList(file, PsiComment::class.java).mapNotNull(::parse)
        CachedValueProvider.Result.create(directives, file)
    }

    /** Finds a module by name: `<name>.cddl` next to [file], or anywhere in the project. */
    fun findModule(file: CddlFile, module: String): CddlFile? {
        val psiManager = PsiManager.getInstance(file.project)
        val fileName = if (module.endsWith(".cddl")) module else "$module.cddl"
        val sibling = file.originalFile.virtualFile?.parent?.findChild(fileName)
        if (sibling != null) return psiManager.findFile(sibling) as? CddlFile
        return FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.allScope(file.project))
            .firstNotNullOfOrNull { psiManager.findFile(it) as? CddlFile }
    }

    /**
     * True when [name] may be defined by a module that cannot be found (e.g. one from the `cddlc` collection), in
     * which case an unresolved name is not necessarily an error.
     */
    fun mayComeFromMissingModule(file: CddlFile, name: String): Boolean =
        directives(file).any { findModule(file, it.module) == null && it.moduleNamesFor(name).isNotEmpty() }

    /** Resolves [name] through the directives of [file], transitively. */
    fun resolve(file: CddlFile, name: String): List<CddlRule> = resolve(file, name, mutableSetOf())

    private fun resolve(file: CddlFile, name: String, visited: MutableSet<Pair<CddlFile, String>>): List<CddlRule> {
        if (!visited.add(file to name)) return emptyList()
        val result = mutableListOf<CddlRule>()
        for (directive in directives(file)) {
            val module = findModule(file, directive.module) ?: continue
            for (target in directive.moduleNamesFor(name)) {
                result += module.ruleMap[target] ?: resolve(module, target, visited)
            }
        }
        return result.distinct()
    }

    /** All rule names made visible in [file] by its directives. */
    fun visibleNames(file: CddlFile): Set<String> = CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            visibleNames(file, mutableSetOf()),
            PsiModificationTracker.getInstance(file.project),
        )
    }

    private fun visibleNames(file: CddlFile, visited: MutableSet<CddlFile>): Set<String> {
        if (!visited.add(file)) return emptySet()
        val result = mutableSetOf<String>()
        for (directive in directives(file)) {
            val module = findModule(file, directive.module) ?: continue
            val moduleNames = module.ruleMap.keys + visibleNames(module, visited)
            result += directive.visibleNamesFor(moduleNames)
        }
        return result
    }
}
