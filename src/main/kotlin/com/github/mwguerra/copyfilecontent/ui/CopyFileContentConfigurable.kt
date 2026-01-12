package com.github.mwguerra.copyfilecontent.ui

import com.github.mwguerra.copyfilecontent.CopyFileContentSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.ui.RoundedLineBorder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class CopyFileContentConfigurable(private val project: Project) : Configurable {

    private val settings = CopyFileContentSettings.getInstance(project)

    private val headerFormatArea = styledTextArea()
    private val preTextArea = styledTextArea()
    private val postTextArea = styledTextArea()

    private val extraLineCheckBox = JBCheckBox("Add an extra line between files")
    private val setMaxFilesCheckBox = JBCheckBox("Set maximum number of files to have their content copied")
    private val maxFilesField = JBTextField(10)
    private val maxFileSizeField = JBTextField(10)

    private val warningLabel = bannerLabel(
        html = "<html><b>Warning:</b> Not setting a maximum number of files may cause high memory usage.</html>",
        fg = JBColor(0xA94442, 0xA94442),
        bg = JBColor(0xF2DEDE, 0xF2DEDE),
        borderColor = JBColor(0xEBCCD1, 0xEBCCD1)
    )

    private val infoLabel = bannerLabel(
        html = "<html><b>Info:</b> Please add file extensions to the table above.</html>",
        fg = JBColor(0x31708F, 0x31708F),
        bg = JBColor(0xD9EDF7, 0xD9EDF7),
        borderColor = JBColor(0xBCE8F1, 0xBCE8F1)
    )

    private val showNotificationCheckBox = JBCheckBox("Show notification after copying")
    private val useFilenameFiltersCheckBox = JBCheckBox("Enable file extension filtering")
    private val strictMemoryReadCheckBox =
        JBCheckBox("Strict memory reading (only read from memory if file is open in editor)")

    private val tableModel = DefaultTableModel(arrayOf("File Extensions"), 0)
    private val table = JBTable(tableModel)
    private val addButton = JButton("Add")
    private val removeButton = JButton("Remove")
    private val filenameFiltersPanel = createFilenameFiltersPanel()

    init {
        setupTableButtons()
        setupListeners()
    }

    override fun createComponent(): JComponent {
        reset() // populate UI from persisted state

        return FormBuilder.createFormBuilder()
            .addComponentFillVertically(createSection("Text structure of what's going to the clipboard") { panel ->
                panel.add(createLabeledPanel("Pre Text:", preTextArea))
                panel.add(createLabeledPanel("File Header Format:", headerFormatArea))
                panel.add(createLabeledPanel("Post Text:", postTextArea))
                panel.add(extraLineCheckBox)
            }, 0)
            .addComponentFillVertically(createSection("Constraints for copying") { panel ->
                panel.add(createInlinePanel(createWrappedCheckBoxPanel(setMaxFilesCheckBox), maxFilesField))
                panel.add(createInlinePanel(JLabel(), warningLabel))
                panel.add(createLabeledPanel("Maximum file size (KB):", maxFileSizeField))
                panel.add(createInlinePanel(createWrappedCheckBoxPanel(useFilenameFiltersCheckBox), filenameFiltersPanel))
                panel.add(createInlinePanel(JLabel(), infoLabel))
            }, 0)
            .addComponentFillVertically(createSection("File Reading Behavior") { panel ->
                panel.add(strictMemoryReadCheckBox)
                val helpLabel = JLabel(
                    "<html><small>When enabled, only reads file content from memory if the file is currently open in an editor tab.<br>" +
                            "When disabled, reads from IntelliJ's document cache even if the tab is closed (better performance).</small></html>"
                )
                helpLabel.border = JBUI.Borders.emptyLeft(25)
                panel.add(helpLabel)
            }, 0)
            .addComponentFillVertically(createSection("Information on what has been copied") { panel ->
                panel.add(showNotificationCheckBox)
            }, 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return state.filenameFilters != currentFilters() ||
                headerFormatArea.text != state.headerFormat ||
                preTextArea.text != state.preText ||
                postTextArea.text != state.postText ||
                extraLineCheckBox.isSelected != state.addExtraLineBetweenFiles ||
                setMaxFilesCheckBox.isSelected != state.setMaxFileCount ||
                (setMaxFilesCheckBox.isSelected && maxFilesField.text.toIntOrNull() != state.fileCountLimit) ||
                maxFileSizeField.text.toIntOrNull() != state.maxFileSizeKB ||
                showNotificationCheckBox.isSelected != state.showCopyNotification ||
                useFilenameFiltersCheckBox.isSelected != state.useFilenameFilters ||
                strictMemoryReadCheckBox.isSelected != state.strictMemoryRead
    }

    override fun apply() {
        val state = settings.state
        state.filenameFilters = currentFilters()
        state.headerFormat = headerFormatArea.text
        state.preText = preTextArea.text
        state.postText = postTextArea.text
        state.addExtraLineBetweenFiles = extraLineCheckBox.isSelected
        state.setMaxFileCount = setMaxFilesCheckBox.isSelected
        state.fileCountLimit = maxFilesField.text.toIntOrNull() ?: state.fileCountLimit
        state.maxFileSizeKB = maxFileSizeField.text.toIntOrNull() ?: state.maxFileSizeKB
        state.showCopyNotification = showNotificationCheckBox.isSelected
        state.useFilenameFilters = useFilenameFiltersCheckBox.isSelected
        state.strictMemoryRead = strictMemoryReadCheckBox.isSelected
        updateDynamicVisibility()
    }

    override fun reset() {
        val state = settings.state
        headerFormatArea.text = state.headerFormat
        preTextArea.text = state.preText
        postTextArea.text = state.postText

        extraLineCheckBox.isSelected = state.addExtraLineBetweenFiles
        setMaxFilesCheckBox.isSelected = state.setMaxFileCount
        maxFilesField.text = state.fileCountLimit.toString()
        maxFileSizeField.text = state.maxFileSizeKB.toString()

        showNotificationCheckBox.isSelected = state.showCopyNotification
        useFilenameFiltersCheckBox.isSelected = state.useFilenameFilters
        strictMemoryReadCheckBox.isSelected = state.strictMemoryRead

        tableModel.rowCount = 0
        state.filenameFilters.forEach { tableModel.addRow(arrayOf(it)) }

        updateDynamicVisibility()
        updateInfoLabelVisibility()
    }

    override fun getDisplayName(): String = "Copy File Content Settings"

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

    private fun createSection(title: String, content: (JPanel) -> Unit): JPanel {
        val panel = JPanel(BorderLayout())
        val contentPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyRight(10)
        }

        content(contentPanel)

        // add spacing between children
        for (component in contentPanel.components) {
            if (component is JComponent) component.border = JBUI.Borders.emptyBottom(10)
        }

        panel.add(createSectionDivider(title), BorderLayout.NORTH)
        panel.add(contentPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createSectionDivider(title: String): JPanel =
        JPanel(BorderLayout()).apply {
            border = IdeBorderFactory.createTitledBorder(title, false, JBUI.insetsTop(20))
        }

    private fun createInlinePanel(leftComponent: JComponent, rightComponent: JComponent, spacing: Int = 10): JPanel {
        val panel = JPanel(BorderLayout())

        val leftWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(leftComponent)
            border = JBUI.Borders.emptyRight(spacing)
        }

        val rightWrapper = JPanel(BorderLayout()).apply {
            add(rightComponent, BorderLayout.CENTER)
        }

        panel.add(leftWrapper, BorderLayout.WEST)
        panel.add(rightWrapper, BorderLayout.CENTER)
        return panel
    }

    private fun createWrappedCheckBoxPanel(checkBox: JBCheckBox, paddingTop: Int = 4): JPanel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(paddingTop)
            add(checkBox)
        }

    private fun createLabeledPanel(title: String, component: JComponent): JPanel =
        JPanel(BorderLayout()).apply {
            add(JLabel(title).apply { border = JBUI.Borders.emptyBottom(4) }, BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }

    private fun styledTextArea(rows: Int = 4, cols: Int = 20): JBTextArea =
        JBTextArea(rows, cols).apply {
            border = JBUI.Borders.merge(
                JBUI.Borders.empty(5),
                RoundedLineBorder(JBColor.LIGHT_GRAY, 4, 1),
                true
            )
        }

    private fun bannerLabel(html: String, fg: JBColor, bg: JBColor, borderColor: JBColor): JLabel =
        JLabel(html).apply {
            foreground = fg
            background = bg
            border = JBUI.Borders.compound(
                JBUI.Borders.empty(5),
                BorderFactory.createLineBorder(borderColor)
            )
            isOpaque = true
            isVisible = false
        }
}
