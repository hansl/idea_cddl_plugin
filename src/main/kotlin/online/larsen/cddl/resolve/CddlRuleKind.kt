package online.larsen.cddl.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlGroupEntry
import online.larsen.cddl.psi.CddlParenType
import online.larsen.cddl.psi.CddlRefType
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlType
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.psi.innerType
import online.larsen.cddl.psi.referenceName
import online.larsen.cddl.psi.resolveTargets
import online.larsen.cddl.psi.value

/** Whether a rule names a type or a group. CDDL does not say so syntactically, so it is inferred. */
enum class CddlRuleKind { TYPE, GROUP, UNKNOWN }

val CddlRule.kind: CddlRuleKind
    get() = CachedValuesManager.getCachedValue(this) {
        val kind = RecursionManager.doPreventingRecursion(this, true) { computeKind(this) } ?: CddlRuleKind.UNKNOWN
        CachedValueProvider.Result.create(kind, PsiModificationTracker.getInstance(project))
    }

/** The kind of a socket follows from its name: `$$name` is a group socket, `$name` a type socket. */
fun socketKind(name: String): CddlRuleKind? = when {
    name.startsWith("$$") -> CddlRuleKind.GROUP
    name.startsWith("$") -> CddlRuleKind.TYPE
    else -> null
}

private fun computeKind(rule: CddlRule): CddlRuleKind = rule.name?.let(::socketKind) ?: when (rule.assignment) {
    CddlAssignment.TYPE_CHOICE -> CddlRuleKind.TYPE
    CddlAssignment.GROUP_CHOICE -> CddlRuleKind.GROUP
    CddlAssignment.DEFINE -> when (val value = rule.value) {
        is CddlGroupEntry -> CddlRuleKind.GROUP
        is CddlType -> kindOf(value)
        else -> CddlRuleKind.UNKNOWN
    }
}

private fun kindOf(type: CddlType): CddlRuleKind = when (type) {
    is CddlParenType -> type.innerType?.let(::kindOf) ?: CddlRuleKind.GROUP
    is CddlRefType -> {
        val target = type.nameRef.resolveTargets.firstOrNull { it is CddlRule } as CddlRule?
        target?.kind ?: socketKind(type.nameRef.referenceName) ?: CddlRuleKind.UNKNOWN
    }
    else -> CddlRuleKind.TYPE
}
