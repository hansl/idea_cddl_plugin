package online.larsen.cddl.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlElementFactory
import online.larsen.cddl.psi.CddlNameRef
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.psi.identifier

/** A reference from a name used in a type or group to the rules (or generic parameter) it denotes. */
class CddlReference(element: CddlNameRef) :
    PsiPolyVariantReferenceBase<CddlNameRef>(element, TextRange(0, element.textLength)) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        ResolveCache.getInstance(element.project).resolveWithCaching(this, RESOLVER, false, incompleteCode)

    /** The primary definition: the rule defined with `=` when a name is also extended with `/=` or `//=`. */
    override fun resolve(): PsiElement? {
        val targets = multiResolve(false).mapNotNull { it.element }
        return targets.firstOrNull { it is CddlRule && it.assignment == CddlAssignment.DEFINE } ?: targets.firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        // A name imported with a namespace prefix ("cose.label") keeps its prefix.
        val oldName = (resolve() as? PsiNamedElement)?.name
        val text = element.text
        val newText = if (oldName != null && text != oldName && text.endsWith(".$oldName")) {
            text.removeSuffix(oldName) + newElementName
        } else {
            newElementName
        }
        element.identifier.replace(CddlElementFactory.createIdentifier(element.project, newText))
        return element
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private companion object {
        val RESOLVER = ResolveCache.PolyVariantResolver<CddlReference> { ref, _ ->
            CddlResolver.resolve(ref.element).map(::PsiElementResolveResult).toTypedArray()
        }
    }
}
