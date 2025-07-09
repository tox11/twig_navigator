package com.twig5.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.twig5.settings.TwigSettingsState
import java.io.File

/**
 * A project-level service responsible for finding Twig template files based on a path string.
 */
@Service(Service.Level.PROJECT)
class TemplateResolverService(private val project: Project) {

    fun resolveTemplatePath(templatePath: String, contextFile: PsiFile): PsiFile? {
        val settings = TwigSettingsState.getInstance(project)
        val rootPath = settings.twigRootPath

        // If the root path is not configured in settings, we can't resolve anything.
        if (rootPath.isBlank()) {
            return null
        }

        // Construct the full path to the potential template file.
        // Twig paths often don't include the extension, so we add it.
        val fullPath = if (templatePath.endsWith(".twig")) templatePath else "$templatePath.twig"
        val templateFile = File(rootPath, fullPath)

        if (!templateFile.exists() || !templateFile.isFile) {
            return null
        }

        // Find the virtual file and then convert it to a PsiFile.
        val virtualFile = VfsUtil.findFileByIoFile(templateFile, true) ?: return null
        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    companion object {
        fun getInstance(project: Project): TemplateResolverService {
            return project.getService(TemplateResolverService::class.java)
        }
    }
}