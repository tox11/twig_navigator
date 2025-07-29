package com.twig5.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.twig5.settings.TwigSettingsState
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Paths

/**
 * A project-level service responsible for finding Twig template files based on a path string.
 * Supports both absolute paths and paths relative to the configured template root or project root.
 * Also handles Symfony-style template paths with @ prefix.
 */
@Service(Service.Level.PROJECT)
class TemplateResolverService(private val project: Project) {

    /**
     * Resolves a template path to a PsiFile.
     * @param templatePath The template path to resolve (can be relative, absolute, or use @ prefix)
     * @param contextFile The file from which the reference is being resolved
     * @return The resolved PsiFile, or null if not found
     */
    fun resolveTemplatePath(templatePath: String, contextFile: PsiFile): PsiFile? {
        val settings = TwigSettingsState.getInstance(project)
        val rootPath = settings.twigRootPath

        // If the root path is not configured in settings, we can't resolve anything.
        if (rootPath.isBlank()) {
            return null
        }

        // Normalize the template path
        val normalizedTemplatePath = templatePath.trim().replace('\\', '/')
        
        // Try to resolve the template using the configured root path
        val templateFile = resolveTemplateFile(normalizedTemplatePath, rootPath)
        
        // If not found and the path is not absolute, try resolving relative to the project root
        if (templateFile == null && !Paths.get(normalizedTemplatePath).isAbsolute) {
            val projectRoot = project.basePath ?: return null
            return resolveTemplateFile(normalizedTemplatePath, projectRoot)
        }
        
        return templateFile
    }

    /**
     * Helper method to resolve a template file from a base path.
     * @param templatePath The template path to resolve
     * @param basePath The base directory to resolve from
     * @return The resolved PsiFile, or null if not found
     */
    private fun resolveTemplateFile(templatePath: String, basePath: String): PsiFile? {
        try {
            // Handle paths starting with @ (common in Symfony)
            val pathToResolve = if (templatePath.startsWith('@')) {
                templatePath.substringAfter('@')
            } else {
                templatePath
            }
            
            // Normalize the path and add .twig extension if needed
            val fullPath = if (pathToResolve.endsWith(".twig")) pathToResolve else "$pathToResolve.twig"
            
            // Create a file object and check if it exists
            val templateFile = File(basePath, fullPath).canonicalFile
            
            if (!templateFile.exists() || !templateFile.isFile) {
                return null
            }

            // Find the virtual file and convert it to a PsiFile
            val virtualFile = VfsUtil.findFileByIoFile(templateFile, true) ?: return null
            return PsiManager.getInstance(project).findFile(virtualFile)
        } catch (e: Exception) {
            // Handle any path resolution errors
            return null
        }
    }

    companion object {
        fun getInstance(project: Project): TemplateResolverService {
            return project.getService(TemplateResolverService::class.java)
        }
    }
}