package online.larsen.cddl.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.PsiNavigateUtil
import online.larsen.cddl.CddlIcons
import online.larsen.cddl.psi.CddlArrayType
import online.larsen.cddl.psi.CddlAssignment
import online.larsen.cddl.psi.CddlBarewordMemberKey
import online.larsen.cddl.psi.CddlFile
import online.larsen.cddl.psi.CddlGroup
import online.larsen.cddl.psi.CddlGroupEntry
import online.larsen.cddl.psi.CddlMapType
import online.larsen.cddl.psi.CddlParenType
import online.larsen.cddl.psi.CddlRule
import online.larsen.cddl.psi.CddlTypeMemberKey
import online.larsen.cddl.psi.CddlValueMemberKey
import online.larsen.cddl.psi.assignment
import online.larsen.cddl.psi.entries
import online.larsen.cddl.psi.keyName
import online.larsen.cddl.psi.memberKey
import online.larsen.cddl.psi.value
import javax.swing.Icon

class CddlStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is CddlFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                CddlStructureViewModel(psiFile, editor)

            override fun isRootNodeShown(): Boolean = false
        }
    }
}

class CddlStructureViewModel(file: CddlFile, editor: Editor?) :
    StructureViewModelBase(file, editor, CddlStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSorters(Sorter.ALPHA_SORTER)
        withSuitableClasses(CddlRule::class.java, CddlGroupEntry::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

/** The structure of a CDDL file: its rules and, nested below them, the keyed members of maps, arrays and groups. */
class CddlStructureViewElement(private val element: NavigatablePsiElement) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    // Navigatable.navigate is override-only: plugins must not call it directly.
    override fun navigate(requestFocus: Boolean) = PsiNavigateUtil.navigate(element, requestFocus)

    override fun canNavigate(): Boolean = element.canNavigate()

    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

    override fun getAlphaSortKey(): String = presentableText ?: ""

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = this@CddlStructureViewElement.presentableText

        override fun getLocationString(): String? = when (element) {
            is CddlRule -> element.assignment.takeIf { it != CddlAssignment.DEFINE }?.text
            is CddlGroupEntry -> element.type?.text?.let(::abbreviate)
            else -> null
        }

        override fun getIcon(unused: Boolean): Icon? = when (element) {
            is CddlGroupEntry -> CddlIcons.MEMBER
            else -> element.getIcon(0)
        }
    }

    private val presentableText: String?
        get() = when (element) {
            is CddlFile -> element.name
            is CddlRule -> element.name
            is CddlGroupEntry -> when (val key = element.memberKey) {
                is CddlBarewordMemberKey -> key.keyName
                is CddlValueMemberKey -> key.firstChild.text
                is CddlTypeMemberKey -> key.type.text.let(::abbreviate)
                else -> null
            }
            else -> null
        }

    override fun getChildren(): Array<StructureViewTreeElement> {
        val children = when (element) {
            is CddlFile -> element.rules
            is CddlRule -> keyedEntries(element.value)
            is CddlGroupEntry -> keyedEntries(element.type)
            else -> emptyList()
        }
        return children.filterIsInstance<NavigatablePsiElement>().map(::CddlStructureViewElement).toTypedArray()
    }

    private fun keyedEntries(value: Any?): List<CddlGroupEntry> {
        val group: CddlGroup? = when (value) {
            is CddlMapType -> value.group
            is CddlArrayType -> value.group
            is CddlParenType -> value.group
            is CddlGroupEntry -> return listOf(value).filter { it.memberKey != null }
            else -> null
        }
        return group?.entries.orEmpty().flatMap { entry ->
            when {
                entry.memberKey != null -> listOf(entry)
                // An inline group, e.g. `? (a: int, b: int)`: show its members directly.
                entry.type is CddlParenType -> keyedEntries(entry.type)
                else -> emptyList()
            }
        }
    }

    private fun abbreviate(text: String): String {
        val singleLine = text.replace(Regex("""\s+"""), " ")
        return if (singleLine.length > 40) singleLine.take(37) + "..." else singleLine
    }
}
