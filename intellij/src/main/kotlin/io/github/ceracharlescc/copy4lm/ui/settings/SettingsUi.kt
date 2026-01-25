package io.github.ceracharlescc.copy4lm.ui.settings

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

internal object SettingsUi {

    fun createSection(title: String, content: (JPanel) -> Unit): JPanel {
        val panel = JPanel(BorderLayout())
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
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

    fun createSectionDivider(title: String): JPanel =
        JPanel(BorderLayout()).apply {
            border = IdeBorderFactory.createTitledBorder(title, false, JBUI.insetsTop(20))
        }

    fun createInlinePanel(leftComponent: JComponent, rightComponent: JComponent, spacing: Int = 10): JPanel {
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

    fun createWrappedCheckBoxPanel(checkBox: JBCheckBox, paddingTop: Int = 4): JPanel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(paddingTop)
            add(checkBox)
        }

    fun createLabeledPanel(title: String, component: JComponent): JPanel =
        JPanel(BorderLayout()).apply {
            add(JLabel(title).apply { border = JBUI.Borders.emptyBottom(4) }, BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }

    fun styledTextArea(rows: Int = 4, cols: Int = 20): JBTextArea =
        JBTextArea(rows, cols).apply {
            border = JBUI.Borders.merge(
                JBUI.Borders.empty(5),
                RoundedLineBorder(JBColor.LIGHT_GRAY, 4, 1),
                true
            )
        }

    fun bannerLabel(html: String, fg: JBColor, bg: JBColor, borderColor: JBColor): JLabel =
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

    fun warningBanner(html: String): JLabel = bannerLabel(
        html = html,
        fg = JBColor(0xA94442, 0xA94442),
        bg = JBColor(0xF2DEDE, 0xF2DEDE),
        borderColor = JBColor(0xEBCCD1, 0xEBCCD1)
    )

    fun infoBanner(html: String): JLabel = bannerLabel(
        html = html,
        fg = JBColor(0x31708F, 0x31708F),
        bg = JBColor(0xD9EDF7, 0xD9EDF7),
        borderColor = JBColor(0xBCE8F1, 0xBCE8F1)
    )
}
