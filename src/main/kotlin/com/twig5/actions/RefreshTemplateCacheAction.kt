package com.twig5.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.twig5.services.TemplateResolverService

/**
 * Action to refresh the Twig template cache
 */
class RefreshTemplateCacheAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Refresh the VFS to ensure we have the latest file system state
        VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
        
        // Show a notification that the cache was refreshed
        Messages.showInfoMessage(
            project,
            "Twig template cache has been refreshed.",
            "Cache Refreshed"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
}
