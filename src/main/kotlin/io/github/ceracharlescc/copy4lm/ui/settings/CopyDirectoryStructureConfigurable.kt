package io.github.ceracharlescc.copy4lm.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import javax.swing.JComponent
import javax.swing.JLabel

internal class CopyDirectoryStructureConfigurable(private val project: Project) : Configurable {

    companion object {
        const val DISPLAY_NAME = "Copy Directory Structure to Clipboard"
        const val ID = "CopyDirectoryStructureConfigurable"
        const val PARENT_ID = Copy4LMConfigurable.ID
    }

    private val settings = Copy4LMSettings.getInstance(project)

    private val preTextArea = SettingsUi.styledTextArea()
    private val postTextArea = SettingsUi.styledTextArea()

    override fun createComponent(): JComponent {
        reset()

        return FormBuilder.createFormBuilder()
            .addComponentFillVertically(SettingsUi.createSection("Directory structure text format") { panel ->
                val explanationLabel = JLabel(
                    "<html><small>The directory structure is inserted between pre-text and post-text.</small></html>"
                )
                explanationLabel.border = JBUI.Borders.emptyBottom(4)
                panel.add(explanationLabel)

                val placeholderHelpLabel = JLabel(
                    $$"<html><small>Available placeholders: <code>$PROJECT_NAME</code>, <code>$DIRECTORY_STRUCTURE</code></small></html>"
                )
                placeholderHelpLabel.border = JBUI.Borders.emptyBottom(8)
                panel.add(placeholderHelpLabel)

                panel.add(SettingsUi.createLabeledPanel("Pre Text:", preTextArea))
                panel.add(SettingsUi.createLabeledPanel("Post Text:", postTextArea))
            }, 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state.directoryStructure
        return preTextArea.text != state.preText ||
                postTextArea.text != state.postText
    }

    override fun apply() {
        val state = settings.state.directoryStructure
        state.preText = preTextArea.text
        state.postText = postTextArea.text
    }

    override fun reset() {
        val state = settings.state.directoryStructure
        preTextArea.text = state.preText
        postTextArea.text = state.postText
    }

    override fun getDisplayName(): String = DISPLAY_NAME
}
