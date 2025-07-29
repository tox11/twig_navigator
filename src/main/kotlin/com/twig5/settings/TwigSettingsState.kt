package com.twig5.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Paths

/**
 * This class handles the persistent storage of the plugin's settings using the
 * standard IntelliJ Platform mechanism (PersistentStateComponent).
 * It manages the Twig template root path configuration.
 */
@State(
    name = "com.twig5.settings.TwigSettingsState",
    storages = [Storage("twigNavigatorSettings.xml")]
)
class TwigSettingsState : PersistentStateComponent<TwigSettingsState> {

    /**
     * The root path where Twig templates are stored.
     * Can be an absolute path or a path relative to the project root.
     */
    var twigRootPath: String = ""
        private set

    /**
     * Sets the Twig root path after validating it.
     * @param path The path to set as the Twig root
     * @return true if the path was set successfully, false if invalid
     */
    fun setTwigRootPath(path: String): Boolean {
        val normalizedPath = path.trim()
        if (normalizedPath.isBlank()) {
            twigRootPath = ""
            return true
        }

        // Check if the path is valid
        return try {
            Paths.get(normalizedPath)
            twigRootPath = normalizedPath
            true
        } catch (e: InvalidPathException) {
            false
        }
    }

    /**
     * Gets the effective Twig root path, resolving it against the project root if it's a relative path.
     * @param project The current project
     * @return The resolved absolute path, or null if invalid
     */
    fun getResolvedTwigRootPath(project: Project): String? {
        if (twigRootPath.isBlank()) return null

        return try {
            val path = Paths.get(twigRootPath)
            if (path.isAbsolute) {
                twigRootPath
            } else {
                // Resolve relative to project root
                project.basePath?.let { basePath ->
                    Paths.get(basePath).resolve(twigRootPath).normalize().toString()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getState(): TwigSettingsState = this

    override fun loadState(state: TwigSettingsState) {
        this.twigRootPath = state.twigRootPath
    }

    companion object {
        /**
         * Gets the service instance for the given project.
         * @param project The project to get settings for
         * @return The settings state instance
         */
        fun getInstance(project: Project): TwigSettingsState {
            return project.getService(TwigSettingsState::class.java)
        }
    }
}