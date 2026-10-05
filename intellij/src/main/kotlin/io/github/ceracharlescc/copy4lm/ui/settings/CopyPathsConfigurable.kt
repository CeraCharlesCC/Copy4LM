package io.github.ceracharlescc.copy4lm.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.FormBuilder
import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import javax.swing.JComponent
import javax.swing.JLabel

internal class CopyPathsConfigurable(project: Project) : Configurable {
    private val settings = Copy4LMSettings.getInstance(project)
    private val startTextArea = SettingsUi.styledTextArea()
    private val endTextArea = SettingsUi.styledTextArea()
    private val delimiterTextArea = SettingsUi.styledTextArea()

    override fun createComponent(): JComponent {
        reset()
        return FormBuilder.createFormBuilder()
            .addComponentFillVertically(SettingsUi.createSection("Path list text format") { panel ->
                panel.add(JLabel("<html><small>Applies to relative and absolute path lists. " +
                    "Text is used exactly as entered, including spaces and newlines.</small></html>"))
                panel.add(SettingsUi.createLabeledPanel("Start:", startTextArea))
                panel.add(SettingsUi.createLabeledPanel("Delimiter:", delimiterTextArea))
                panel.add(SettingsUi.createLabeledPanel("End:", endTextArea))
            }, 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state.pathList
        return startTextArea.text != state.start || endTextArea.text != state.end ||
            delimiterTextArea.text != state.delimiter
    }

    override fun apply() {
        val state = settings.state.pathList
        state.start = startTextArea.text
        state.end = endTextArea.text
        state.delimiter = delimiterTextArea.text
    }

    override fun reset() {
        val state = settings.state.pathList
        startTextArea.text = state.start
        endTextArea.text = state.end
        delimiterTextArea.text = state.delimiter
    }

    override fun getDisplayName(): String = "Copy Path List to Clipboard"
}
