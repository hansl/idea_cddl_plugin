package online.larsen.cddl.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.resolve.CddlReference

abstract class CddlNameRefMixin(node: ASTNode) : ASTWrapperPsiElement(node), CddlNameRef {
    override fun getReference(): CddlReference = CddlReference(this)
}
