package com.twig5.listeners

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.twig5.settings.AppSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for application and project lifecycle events.
 */
class PluginLifecycleListener : AppLifecycleListener {
    private val logger = Logger.getInstance(javaClass)

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        logger.info("Twig Navigator plugin initialized")
        // Ensure our application service is initialized
        AppSettingsState.instance
    }
}

/**
 * Handles project-level initialization.
 */
class ProjectOpenActivity : ProjectActivity {
    private val logger = Logger.getInstance(javaClass)

    override suspend fun execute(project: Project) {
        logger.info("Project opened: ${project.name}")
    }
}
