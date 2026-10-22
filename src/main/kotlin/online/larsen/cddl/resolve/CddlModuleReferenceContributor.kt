package online.larsen.cddl.resolve

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import online.larsen.cddl.CddlFileType
import online.larsen.cddl.CddlLanguage
import online.larsen.cddl.psi.CddlFile

/** Makes the module name of `;# import` and `;# include` directives navigable. */
class CddlModuleReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiComment().withLanguage(CddlLanguage),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val comment = element as? PsiComment ?: return PsiReference.EMPTY_ARRAY
                    val directive = CddlDirectives.parse(comment) ?: return PsiReference.EMPTY_ARRAY
                    return arrayOf(CddlModuleReference(comment, directive))
                }
            },
        )
    }
}

class CddlModuleReference(comment: PsiComment, private val directive: CddlDirective) :
    PsiReferenceBase<PsiComment>(comment, directive.moduleRange, true) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? CddlFile ?: return null
        return CddlDirectives.findModule(file, directive.module)
    }

    override fun getVariants(): Array<Any> {
        val directory = element.containingFile?.originalFile?.virtualFile?.parent ?: return emptyArray()
        return directory.children
            .filter { it.fileType == CddlFileType && it != element.containingFile.originalFile.virtualFile }
            .map { it.nameWithoutExtension }
            .toTypedArray()
    }
}
