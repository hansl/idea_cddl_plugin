package online.larsen.cddl.navigation

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import online.larsen.cddl.CddlBundle
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypes
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.resolve.CddlResolver

/** Links rules defined with `=` to the `/=` and `//=` rules that add choices to them, and back. */
class CddlLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        if (element.node.elementType != CddlTypes.ID) return
        val rule = element.parent as? CddlRule ?: return
        if (rule.nameIdentifier != element) return
        val name = rule.name ?: return
        val file = rule.containingFile as? CddlFile ?: return
        val definitions = CddlResolver.resolveRule(file, name)

        if (rule.assignment == CddlAssignment.DEFINE) {
            val extensions = definitions.filter { it.assignment != CddlAssignment.DEFINE }
            if (extensions.isEmpty()) return
            result += NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(extensions)
                .setTooltipText(CddlBundle.message("line.marker.has.extensions", name))
                .createLineMarkerInfo(element)
        } else {
            val bases = definitions.filter { it.assignment == CddlAssignment.DEFINE }
            if (bases.isEmpty()) return
            result += NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                .setTargets(bases)
                .setTooltipText(CddlBundle.message("line.marker.extends", name))
                .createLineMarkerInfo(element)
        }
    }
}
