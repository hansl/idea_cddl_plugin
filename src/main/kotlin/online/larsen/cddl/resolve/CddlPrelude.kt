package online.larsen.cddl.resolve

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import online.larsen.cddl.CddlFileType
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlRule

/** The standard prelude (RFC 8610, Appendix D), available in every CDDL file. */
@Service(Service.Level.PROJECT)
class CddlPrelude(private val project: Project) {
    val file: CddlFile?
        get() = PsiManager.getInstance(project).findFile(VIRTUAL_FILE) as? CddlFile

    val rules: List<CddlRule>
        get() = file?.rules.orEmpty()

    fun rules(name: String): List<CddlRule> = file?.ruleMap?.get(name).orEmpty()

    companion object {
        const val FILE_NAME: String = "prelude.cddl"

        private val TEXT: String by lazy {
            CddlPrelude::class.java.getResourceAsStream("/cddl/$FILE_NAME")!!.use { it.readBytes().decodeToString() }
        }

        private val VIRTUAL_FILE: LightVirtualFile by lazy {
            LightVirtualFile(FILE_NAME, CddlFileType, TEXT).apply { isWritable = false }
        }

        /** Names defined by the prelude. */
        val NAMES: Set<String> by lazy {
            TEXT.lineSequence()
                .mapNotNull { Regex("""^([A-Za-z@_$][-.\w@$]*)\s*=""").find(it)?.groupValues?.get(1) }
                .toSet()
        }

        fun getInstance(project: Project): CddlPrelude = project.service()

        fun isPrelude(element: PsiElement): Boolean =
            element.containingFile?.originalFile?.virtualFile == VIRTUAL_FILE
    }
}
