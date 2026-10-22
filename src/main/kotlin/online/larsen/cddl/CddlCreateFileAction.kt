package online.larsen.cddl

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CddlCreateFileAction : CreateFileFromTemplateAction(), DumbAware {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(CddlBundle.message("action.new.file.title"))
            .addKind(CddlBundle.message("action.new.file.kind"), CddlIcons.FILE, TEMPLATE_NAME)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
        CddlBundle.message("action.new.file.name", newName)

    companion object {
        const val TEMPLATE_NAME = "CDDL File"
    }
}
