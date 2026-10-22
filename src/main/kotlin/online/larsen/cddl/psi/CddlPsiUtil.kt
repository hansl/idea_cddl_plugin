package online.larsen.cddl.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import online.larsen.cddl.psi.CddlTypes.*
import online.larsen.cddl.resolve.CddlReference

/** How a rule is assigned: a definition (`=`), or a type/group choice extension (`/=`, `//=`). */
enum class CddlAssignment(val text: String) {
    DEFINE("="),
    TYPE_CHOICE("/="),
    GROUP_CHOICE("//="),
}

val CddlRule.assignmentToken: PsiElement?
    get() = node.findChildByType(CddlTokenSets.ASSIGNMENTS)?.psi

val CddlRule.assignment: CddlAssignment
    get() = when (assignmentToken?.node?.elementType) {
        TCHOICE_ASSIGN -> CddlAssignment.TYPE_CHOICE
        GCHOICE_ASSIGN -> CddlAssignment.GROUP_CHOICE
        else -> CddlAssignment.DEFINE
    }

/** The right-hand side of a rule: either a [CddlType] or a [CddlGroupEntry]. */
val CddlRule.value: PsiElement?
    get() = type ?: groupEntry

val CddlRule.genericParamList: List<CddlGenericParam>
    get() = genericParams?.genericParamList.orEmpty()

val CddlGenericParams.genericParamList: List<CddlGenericParam>
    get() = PsiTreeUtil.getChildrenOfTypeAsList(this, CddlGenericParam::class.java)

val CddlGenericArgs.typeList: List<CddlType>
    get() = PsiTreeUtil.getChildrenOfTypeAsList(this, CddlType::class.java)

val CddlNameRef.identifier: PsiElement
    get() = firstChild

val CddlNameRef.referenceName: String
    get() = text

val CddlNameRef.cddlReference: CddlReference
    get() = reference as CddlReference

/** The elements this name resolves to: rules, or a generic parameter. */
val CddlNameRef.resolveTargets: List<PsiElement>
    get() = cddlReference.multiResolve(false).mapNotNull { it.element }

/** Generic arguments applied to the referenced name, e.g. `<tstr>` in `message<tstr>`. */
val CddlNameRef.genericArgs: CddlGenericArgs?
    get() = PsiTreeUtil.getNextSiblingOfType(this, CddlGenericArgs::class.java)

val CddlGroupEntry.memberKey: CddlMemberKey?
    get() = barewordMemberKey ?: valueMemberKey ?: typeMemberKey

val CddlBarewordMemberKey.identifier: PsiElement
    get() = firstChild

val CddlBarewordMemberKey.keyName: String
    get() = identifier.text

val CddlControlType.operatorToken: PsiElement?
    get() = node.findChildByType(CTLOP)?.psi

/** The control operator name, including the leading dot, e.g. `.size`. */
val CddlControlType.operatorName: String?
    get() = operatorToken?.text

/** The type being controlled (left-hand side). */
val CddlControlType.target: CddlType?
    get() = typeList.getOrNull(0)

/** The controller type (right-hand side). */
val CddlControlType.controller: CddlType?
    get() = typeList.getOrNull(1)

val CddlLiteralType.literalToken: PsiElement
    get() = firstChild

val CddlLiteralType.literalTokenType: IElementType
    get() = literalToken.node.elementType

val CddlLiteralType.isTextString: Boolean
    get() = literalTokenType == TEXT_STRING

val CddlGroup.entries: List<CddlGroupEntry>
    get() = groupChoiceList.flatMap { it.groupEntryList }

/**
 * Returns the type between parentheses when they hold a single plain type (`(a / b)`), or `null` when they hold a
 * group (`(a: int, b: int)`).
 */
val CddlParenType.innerType: CddlType?
    get() {
        val choices = group?.groupChoiceList ?: return null
        val entry = choices.singleOrNull()?.groupEntryList?.singleOrNull() ?: return null
        if (entry.occurrence != null || entry.memberKey != null) return null
        return entry.type
    }

/** The rule containing this element, if any. */
val PsiElement.containingRule: CddlRule?
    get() = PsiTreeUtil.getParentOfType(this, CddlRule::class.java, false)
