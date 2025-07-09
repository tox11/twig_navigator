package com.twig5.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.twig5.settings.TwigSettingsConfigurable

/**
 * Action to open the Twig Navigator settings
 */
class OpenSettingsAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Используем рекомендованный публичный API для отображения диалога настроек.
        // Этот метод специально предназначен для открытия настроек по классу Configurable.
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TwigSettingsConfigurable::class.java)
    }
}