package online.larsen.cddl.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import online.larsen.cddl.CddlLanguage

class CddlLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = CddlLanguage

    override fun getConfigurableDisplayName(): String = "CDDL"

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 4
        indentOptions.TAB_SIZE = 2
        commonSettings.KEEP_BLANK_LINES_IN_CODE = 2
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
            consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE")
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = """
        ; A person
        person = {
          name: tstr .size (1..64),
          ? age: uint,
          contact,
          * tstr => any
        }

        contact = (
          email: tstr //
          phone: tstr
        )

        message<T> = [id: uint, body: T]
        COSE_Untagged_Message = COSE_Sign / COSE_Sign1 /
            COSE_Encrypt / COSE_Encrypt0
    """.trimIndent()

    override fun createConfigurable(
        baseSettings: CodeStyleSettings,
        modelSettings: CodeStyleSettings,
    ): CodeStyleConfigurable = object : CodeStyleAbstractConfigurable(baseSettings, modelSettings, "CDDL") {
        override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
            object : TabbedLanguageCodeStylePanel(CddlLanguage, currentSettings, settings) {
                override fun initTabs(settings: CodeStyleSettings) {
                    addIndentOptionsTab(settings)
                    addBlankLinesTab(settings)
                }
            }
    }
}
