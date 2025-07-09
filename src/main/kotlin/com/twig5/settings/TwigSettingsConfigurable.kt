package com.twig5.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class TwigSettingsConfigurable(private val project: Project) : Configurable {

    private lateinit var settingsPanel: JPanel
    private lateinit var twigRootPathField: TextFieldWithBrowseButton
    // FIX: Now it can correctly find and use TwigSettingsState
    private val settings = TwigSettingsState.getInstance(project)

    override fun getDisplayName(): String = "Twig Navigator Settings"

    override fun createComponent(): JComponent {
        twigRootPathField = TextFieldWithBrowseButton()
        twigRootPathField.addBrowseFolderListener(
            "Select Twig Templates Root Directory",
            "Please select the root folder containing your Twig templates.",
            project,
            FileChooserDescriptor(
                false, // chooseFiles
                true,  // chooseFolders
                false, // chooseJars
                false, // chooseJarsAsFiles
                false, // chooseJarContents
                false  // chooseMultiple
            )
        )

        settingsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Templates root path:"), twigRootPathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return settingsPanel
    }

    override fun isModified(): Boolean {
        return twigRootPathField.text != settings.twigRootPath
    }

    override fun apply() {
        // FIX: This assignment now works because twigRootPath is a 'var'
        settings.twigRootPath = twigRootPathField.text
    }

    override fun reset() {
        twigRootPathField.text = settings.twigRootPath
    }
}