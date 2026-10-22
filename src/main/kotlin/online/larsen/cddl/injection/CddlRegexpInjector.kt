package online.larsen.cddl.injection

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import online.larsen.cddl.CddlControlOperators
import online.larsen.cddl.psi.CddlControlType
import online.larsen.cddl.psi.CddlStringUtil
import online.larsen.cddl.psi.controller
import online.larsen.cddl.psi.impl.CddlLiteralTypeMixin
import online.larsen.cddl.psi.isTextString
import online.larsen.cddl.psi.operatorName
import org.intellij.lang.regexp.RegExpLanguage

/** Injects the RegExp language into the controller of `.regexp` and `.pcre`, e.g. `tstr .regexp "[a-z]+"`. */
class CddlRegexpInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val literal = context as? CddlLiteralTypeMixin ?: return
        if (!literal.isTextString) return
        val control = literal.parent as? CddlControlType ?: return
        if (control.controller != literal || control.operatorName !in CddlControlOperators.REGEXP_OPERATORS) return
        registrar.startInjecting(RegExpLanguage.INSTANCE)
            .addPlace(null, null, literal, CddlStringUtil.contentRange(literal.text))
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(CddlLiteralTypeMixin::class.java)
}
