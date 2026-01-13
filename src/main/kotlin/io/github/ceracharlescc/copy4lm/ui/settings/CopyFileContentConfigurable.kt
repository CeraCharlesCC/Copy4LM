package io.github.ceracharlescc.copy4lm.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import javax.swing.JComponent
import javax.swing.JLabel

internal class CopyFileContentConfigurable(private val project: Project) : Configurable {

    companion object {
        const val DISPLAY_NAME = "Copy File Content to Clipboard"
        const val ID = "CopyFileContentConfigurable"
        const val PARENT_ID = Copy4LMConfigurable.ID
    }

    private val settings = Copy4LMSettings.getInstance(project)

    private val headerFormatArea = SettingsUi.styledTextArea()
    private val footerFormatArea = SettingsUi.styledTextArea()
    private val preTextArea = SettingsUi.styledTextArea()
    private val postTextArea = SettingsUi.styledTextArea()
    private val extraLineCheckBox = JBCheckBox("Add an extra line between files")

    override fun createComponent(): JComponent {
        reset()

        return FormBuilder.createFormBuilder()
            .addComponentFillVertically(SettingsUi.createSection("Text structure of what's going to the clipboard") { panel ->
                val placeholderHelpLabel = JLabel(
                    $$"<html><small>Available placeholders: <code>$PROJECT_NAME</code>, <code>$FILE_PATH</code>, <code>$DIRECTORY_STRUCTURE</code></small></html>"
                )
                placeholderHelpLabel.border = JBUI.Borders.emptyBottom(8)
                panel.add(placeholderHelpLabel)
                panel.add(SettingsUi.createLabeledPanel("Pre Text:", preTextArea))
                panel.add(SettingsUi.createLabeledPanel("File Header Format:", headerFormatArea))
                panel.add(SettingsUi.createLabeledPanel("File Footer Format:", footerFormatArea))
                panel.add(SettingsUi.createLabeledPanel("Post Text:", postTextArea))
                panel.add(extraLineCheckBox)
            }, 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state.fileContent
        return headerFormatArea.text != state.headerFormat ||
                footerFormatArea.text != state.footerFormat ||
                preTextArea.text != state.preText ||
                postTextArea.text != state.postText ||
                extraLineCheckBox.isSelected != state.addExtraLineBetweenFiles
    }

    override fun apply() {
        val state = settings.state.fileContent
        state.headerFormat = headerFormatArea.text
        state.footerFormat = footerFormatArea.text
        state.preText = preTextArea.text
        state.postText = postTextArea.text
        state.addExtraLineBetweenFiles = extraLineCheckBox.isSelected
    }

    override fun reset() {
        val state = settings.state.fileContent
        headerFormatArea.text = state.headerFormat
        footerFormatArea.text = state.footerFormat
        preTextArea.text = state.preText
        postTextArea.text = state.postText
        extraLineCheckBox.isSelected = state.addExtraLineBetweenFiles
    }

    override fun getDisplayName(): String = DISPLAY_NAME
}
