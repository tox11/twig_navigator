package com.twig5.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * This class handles the persistent storage of the plugin's settings using the
 * standard IntelliJ Platform mechanism (PersistentStateComponent).
 */
@State(
    name = "com.twig5.settings.TwigSettingsState",
    storages = [Storage("twigNavigatorSettings.xml")]
)
class TwigSettingsState : PersistentStateComponent<TwigSettingsState> {

    // FIX: This must be a 'var' to be mutable and have a default value.
    var twigRootPath: String = ""

    override fun getState(): TwigSettingsState {
        return this
    }

    override fun loadState(state: TwigSettingsState) {
        this.twigRootPath = state.twigRootPath
    }

    companion object {
        fun getInstance(project: Project): TwigSettingsState {
            return project.getService(TwigSettingsState::class.java)
        }
    }
}