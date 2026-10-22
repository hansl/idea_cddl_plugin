package online.larsen.cddl

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CddlIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/cddl.svg", CddlIcons::class.java)

    @JvmField
    val TYPE_RULE: Icon = AllIcons.Nodes.Type

    @JvmField
    val GROUP_RULE: Icon = AllIcons.Nodes.Record

    @JvmField
    val GENERIC_PARAMETER: Icon = AllIcons.Nodes.Parameter

    @JvmField
    val MEMBER: Icon = AllIcons.Nodes.Field

    @JvmField
    val CONTROL_OPERATOR: Icon = AllIcons.Nodes.Function
}
