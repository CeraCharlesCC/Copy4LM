package io.github.ceracharlescc.copy4lm.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import io.github.ceracharlescc.copy4lm.Copy4LMSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

internal class Copy4LMConfigurable(private val project: Project) : Configurable {

    companion object {
        const val DISPLAY_NAME = "Copy 4 LM Settings"
        const val ID = "Copy4LMConfigurable"
    }

    private val settings = Copy4LMSettings.getInstance(project)

    // Constraints for copying
    private val setMaxFilesCheckBox = JBCheckBox("Set maximum number of files to have their content copied")
    private val maxFilesField = JBTextField(10)
    private val maxFileSizeField = JBTextField(10)
    private val useFilenameFiltersCheckBox = JBCheckBox("Enable file extension filtering")
    private val tableModel = DefaultTableModel(arrayOf("File Extensions"), 0)
    private val table = JBTable(tableModel)
    private val addButton = JButton("Add")
    private val removeButton = JButton("Remove")
    private val filenameFiltersPanel = createFilenameFiltersPanel()

    private val warningLabel = SettingsUi.warningBanner(
        "<html><b>Warning:</b> Not setting a maximum number of files may cause high memory usage.</html>"
    )
    private val infoLabel = SettingsUi.infoBanner(
        "<html><b>Info:</b> Please add file extensions to the table above.</html>"
    )

    // File reading behavior
    private val strictMemoryReadCheckBox =
        JBCheckBox("Strict memory reading (only read from memory if file is open in editor)")

    // Information on what has been copied
    private val showNotificationCheckBox = JBCheckBox("Show notification after copying")

    init {
        setupTableButtons()
        setupListeners()
    }

    override fun createComponent(): JComponent {
        reset()

        return FormBuilder.createFormBuilder()
            .addComponentFillVertically(SettingsUi.createSection("Constraints for copying") { panel ->
                panel.add(SettingsUi.createInlinePanel(SettingsUi.createWrappedCheckBoxPanel(setMaxFilesCheckBox), maxFilesField))
                panel.add(SettingsUi.createInlinePanel(JLabel(), warningLabel))
                panel.add(SettingsUi.createLabeledPanel("Maximum file size (KB):", maxFileSizeField))
                panel.add(
                    SettingsUi.createInlinePanel(
                        SettingsUi.createWrappedCheckBoxPanel(useFilenameFiltersCheckBox),
                        filenameFiltersPanel
                    )
                )
                panel.add(SettingsUi.createInlinePanel(JLabel(), infoLabel))
            }, 0)
            .addComponentFillVertically(SettingsUi.createSection("File Reading Behavior") { panel ->
                panel.add(strictMemoryReadCheckBox)
                val helpLabel = JLabel(
                    "<html><small>When enabled, only reads file content from memory if the file is currently open in an editor tab.<br>" +
                            "When disabled, reads from IntelliJ's document cache even if the tab is closed (better performance).</small></html>"
                )
                helpLabel.border = JBUI.Borders.emptyLeft(25)
                panel.add(helpLabel)
            }, 0)
            .addComponentFillVertically(SettingsUi.createSection("Information on what has been copied") { panel ->
                panel.add(showNotificationCheckBox)
            }, 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state.common
        return state.filenameFilters != currentFilters() ||
                setMaxFilesCheckBox.isSelected != state.setMaxFileCount ||
                (setMaxFilesCheckBox.isSelected && maxFilesField.text.toIntOrNull() != state.fileCountLimit) ||
                maxFileSizeField.text.toIntOrNull() != state.maxFileSizeKB ||
                useFilenameFiltersCheckBox.isSelected != state.useFilenameFilters ||
                strictMemoryReadCheckBox.isSelected != state.strictMemoryRead ||
                showNotificationCheckBox.isSelected != state.showCopyNotification
    }

    override fun apply() {
        val state = settings.state.common
        state.filenameFilters = currentFilters()
        state.setMaxFileCount = setMaxFilesCheckBox.isSelected
        state.fileCountLimit = maxFilesField.text.toIntOrNull() ?: state.fileCountLimit
        state.maxFileSizeKB = maxFileSizeField.text.toIntOrNull() ?: state.maxFileSizeKB
        state.useFilenameFilters = useFilenameFiltersCheckBox.isSelected
        state.strictMemoryRead = strictMemoryReadCheckBox.isSelected
        state.showCopyNotification = showNotificationCheckBox.isSelected
        updateDynamicVisibility()
    }

    override fun reset() {
        val state = settings.state.common
        setMaxFilesCheckBox.isSelected = state.setMaxFileCount
        maxFilesField.text = state.fileCountLimit.toString()
        maxFileSizeField.text = state.maxFileSizeKB.toString()
        useFilenameFiltersCheckBox.isSelected = state.useFilenameFilters
        strictMemoryReadCheckBox.isSelected = state.strictMemoryRead
        showNotificationCheckBox.isSelected = state.showCopyNotification

        tableModel.rowCount = 0
        state.filenameFilters.forEach { tableModel.addRow(arrayOf(it)) }

        updateDynamicVisibility()
        updateInfoLabelVisibility()
    }

    override fun getDisplayName(): String = DISPLAY_NAME

    // ---------- UI helpers ----------

    private fun setupListeners() {
        setMaxFilesCheckBox.addActionListener { updateDynamicVisibility() }
        useFilenameFiltersCheckBox.addActionListener {
            updateDynamicVisibility()
            updateInfoLabelVisibility()
        }
    }

    private fun updateDynamicVisibility() {
        val maxFilesSelected = setMaxFilesCheckBox.isSelected
        maxFilesField.isVisible = maxFilesSelected
        warningLabel.isVisible = !maxFilesSelected

        filenameFiltersPanel.isVisible = useFilenameFiltersCheckBox.isSelected
        updateInfoLabelVisibility()
    }

    private fun updateInfoLabelVisibility() {
        infoLabel.isVisible = useFilenameFiltersCheckBox.isSelected && tableModel.rowCount == 0
    }

    private fun currentFilters(): List<String> =
        List(tableModel.rowCount) { row -> tableModel.getValueAt(row, 0).toString().trim() }
            .filter { it.isNotBlank() }

    private fun setupTableButtons() {
        addButton.addActionListener {
            val extension = Messages.showInputDialog("Enter file extension:", "Add Filter", null)
            if (!extension.isNullOrBlank()) {
                tableModel.addRow(arrayOf(extension.trim()))
                updateInfoLabelVisibility()
            }
        }

        removeButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow)
                updateInfoLabelVisibility()
            }
        }
    }

    private fun createFilenameFiltersPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(250, 100)
        panel.add(scrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }
}
