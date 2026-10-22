package online.larsen.cddl.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.CddlFileType
import online.larsen.cddl.CddlLanguage

class CddlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, CddlLanguage) {
    override fun getFileType(): FileType = CddlFileType

    override fun toString(): String = "CDDL File"

    val rules: List<CddlRule>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, CddlRule::class.java)

    /** The first rule of a CDDL file is its root. */
    val rootRule: CddlRule?
        get() = PsiTreeUtil.getChildOfType(this, CddlRule::class.java)

    /** All rules of this file by name; a name maps to several rules when it is extended with `/=` or `//=`. */
    val ruleMap: Map<String, List<CddlRule>>
        get() = CachedValuesManager.getCachedValue(this) {
            val map = rules.filter { it.name != null }.groupBy { it.name!! }
            CachedValueProvider.Result.create(map, this)
        }
}
