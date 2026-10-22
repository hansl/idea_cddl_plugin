package online.larsen.cddl.psi

import com.intellij.psi.tree.IElementType
import online.larsen.cddl.CddlLanguage
import org.jetbrains.annotations.NonNls

class CddlTokenType(@NonNls debugName: String) : IElementType(debugName, CddlLanguage)

class CddlElementType(@NonNls debugName: String) : IElementType(debugName, CddlLanguage)
