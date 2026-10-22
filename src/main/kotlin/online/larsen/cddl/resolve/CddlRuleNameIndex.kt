package online.larsen.cddl.resolve

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import online.larsen.cddl.CddlFileType
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlRule

/** Indexes the names of the rules defined by each CDDL file. */
class CddlRuleNameIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = NAME

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { content ->
        val file = content.psiFile as? CddlFile ?: return@DataIndexer emptyMap()
        file.rules.mapNotNull { it.name }.associateWith { null }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(CddlFileType)

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        val NAME: ID<String, Void> = ID.create("online.larsen.cddl.ruleNames")

        fun allNames(project: Project): Collection<String> {
            if (DumbService.isDumb(project)) return emptyList()
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
        }

        fun findRules(project: Project, name: String, scope: GlobalSearchScope): List<CddlRule> {
            if (DumbService.isDumb(project)) return emptyList()
            val psiManager = PsiManager.getInstance(project)
            return FileBasedIndex.getInstance().getContainingFiles(NAME, name, scope)
                .mapNotNull { psiManager.findFile(it) as? CddlFile }
                .flatMap { it.ruleMap[name].orEmpty() }
        }
    }
}
