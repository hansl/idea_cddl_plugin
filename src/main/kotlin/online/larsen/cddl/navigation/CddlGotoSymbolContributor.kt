package online.larsen.cddl.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import online.larsen.cddl.resolve.CddlRuleNameIndex

/** Makes CDDL rules available in "Go to Symbol". */
class CddlGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        FileBasedIndex.getInstance().processAllKeys(CddlRuleNameIndex.NAME, processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        for (rule in CddlRuleNameIndex.findRules(parameters.project, name, parameters.searchScope)) {
            if (!processor.process(rule)) return
        }
    }
}
