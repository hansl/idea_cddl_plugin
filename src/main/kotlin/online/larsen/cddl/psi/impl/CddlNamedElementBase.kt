package online.larsen.cddl.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import online.larsen.cddl.psi.CddlElementFactory
import online.larsen.cddl.psi.CddlNamedElement
import online.larsen.cddl.psi.CddlTypes
import javax.swing.Icon

/** Common implementation for declarations whose name is their `ID` child. */
abstract class CddlNamedElementBase(node: ASTNode) : ASTWrapperPsiElement(node), CddlNamedElement {
    override fun getNameIdentifier(): PsiElement? = node.findChildByType(CddlTypes.ID)?.psi

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(CddlElementFactory.createIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = name

        override fun getLocationString(): String? = containingFile?.name

        override fun getIcon(unused: Boolean): Icon? = this@CddlNamedElementBase.getIcon(0)
    }
}
