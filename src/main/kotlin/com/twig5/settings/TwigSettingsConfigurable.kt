package com.twig5.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Provides the configuration UI for the Twig Navigator plugin.
 * Allows users to set the root directory for Twig templates.
 */
class TwigSettingsConfigurable(private val project: Project) : Configurable {

    private lateinit var settingsPanel: JPanel
    private lateinit var twigRootPathField: TextFieldWithBrowseButton
    private val settings = TwigSettingsState.getInstance(project)
    
    private var lastValidPath: String = settings.twigRootPath

    override fun getDisplayName(): String = "Twig Navigator Settings"

    override fun createComponent(): JComponent {
        twigRootPathField = TextFieldWithBrowseButton()
        
        // Configure the folder chooser dialog
        twigRootPathField.addBrowseFolderListener(
            "Select Twig Templates Root Directory",
            "Please select the root folder containing your Twig templates.\n" +
                    "This can be an absolute path or relative to the project root.",
            project,
            FileChooserDescriptor(
                false, // chooseFiles
                true,  // chooseFolders
                false, // chooseJars
                false, // chooseJarsAsFiles
                false, // chooseJarContents
                false  // chooseMultiple
            ).apply {
                title = "Select Twig Templates Root Directory"
                description = "This directory will be used as the root for template resolution."
            }
        )
        
        // Add validation when the field loses focus
        twigRootPathField.textField.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                validatePath()
            }
        })

        // Build the settings panel
        settingsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Templates root path:"), 
                twigRootPathField, 
                1, 
                false
            )
            .addComponentFillVertically(JPanel(), 10)
            .panel

        return settingsPanel
    }
    
    /**
     * Validates the current path in the text field and shows an error if invalid.
     * @return true if the path is valid, false otherwise
     */
    private fun validatePath(): Boolean {
        val path = twigRootPathField.text.trim()
        if (path.isBlank()) {
            return true // Empty path is valid (will use project root)
        }
        
        return try {
            val file = File(path)
            if (!file.isAbsolute) {
                // For relative paths, check if we can resolve it against the project root
                project.basePath?.let { basePath ->
                    File(basePath, path).canonicalFile
                } ?: throw IllegalStateException("Project has no base path")
            } else {
                file.canonicalFile
            }
            lastValidPath = path
            true
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "The specified path is not valid: ${e.message}",
                "Invalid Path"
            )
            false
        }
    }

    override fun isModified(): Boolean {
        return twigRootPathField.text != settings.twigRootPath
    }

    override fun apply() {
        if (validatePath()) {
            // Only update if validation passes
            if (!settings.setTwigRootPath(twigRootPathField.text)) {
                // If setting the path failed, revert to the last valid path
                twigRootPathField.text = lastValidPath
                Messages.showErrorDialog(
                    project,
                    "Failed to set the template root path. Please check the path and try again.",
                    "Error Saving Settings"
                )
            }
        } else {
            // If validation fails, revert to the last valid path
            twigRootPathField.text = lastValidPath
        }
    }

    override fun reset() {
        twigRootPathField.text = settings.twigRootPath
        lastValidPath = settings.twigRootPath
    }
}