package online.larsen.cddl

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object CddlFileType : LanguageFileType(CddlLanguage) {
    override fun getName(): String = "CDDL"

    override fun getDescription(): String = CddlBundle.message("filetype.cddl.description")

    override fun getDefaultExtension(): String = "cddl"

    override fun getIcon(): Icon = CddlIcons.FILE
}
