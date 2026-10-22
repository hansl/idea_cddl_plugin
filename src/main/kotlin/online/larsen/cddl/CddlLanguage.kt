package online.larsen.cddl

import com.intellij.lang.Language

object CddlLanguage : Language("CDDL") {
    private fun readResolve(): Any = CddlLanguage

    override fun getDisplayName(): String = "CDDL"
}
