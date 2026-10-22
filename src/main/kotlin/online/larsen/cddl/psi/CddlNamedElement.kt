package online.larsen.cddl.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/** A declaration that introduces a name: a rule or a generic parameter. */
interface CddlNamedElement : PsiNameIdentifierOwner, NavigatablePsiElement

/** The key part of a group entry: `name:`, `"value":` or `type =>`. */
interface CddlMemberKey : PsiElement
