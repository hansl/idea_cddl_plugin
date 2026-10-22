package online.larsen.cddl.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.CddlFileType

object CddlElementFactory {
    fun createFile(project: Project, text: String): CddlFile =
        PsiFileFactory.getInstance(project).createFileFromText("dummy.cddl", CddlFileType, text) as CddlFile

    fun createIdentifier(project: Project, name: String): PsiElement =
        createRule(project, "$name = any").nameIdentifier
            ?: error("Cannot create an identifier from '$name'")

    fun createRule(project: Project, text: String): CddlRule =
        createFile(project, text).rules.firstOrNull() ?: error("Cannot create a rule from '$text'")

    fun createType(project: Project, text: String): CddlType =
        createRule(project, "dummy = $text").type ?: error("Cannot create a type from '$text'")

    fun createNewline(project: Project): PsiElement =
        PsiTreeUtil.nextLeaf(createFile(project, "a = any\nb = any").rules.first())
            ?: error("Cannot create a newline")
}
