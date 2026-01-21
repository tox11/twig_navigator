package com.twig5.references

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.twig.TwigLanguage
import com.twig5.services.TemplateResolverService
import com.twig5.references.TwigPhpNavigationUtils

class TwigGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        if (element == null) return null
        val file = element.containingFile ?: return null
        val project: Project = file.project

        // Only act in Twig files
        if (file.language !is TwigLanguage) return null

        // Find the leaf at caret
        val leaf = file.findElementAt(offset) ?: return null

        // 1. Template path resolution (typically in quotes)
        val tokenText = buildTokenText(leaf)
        val relativeOffset = offset - leaf.textRange.startOffset
        val inQuotes = relativeOffset >= 0 && relativeOffset <= tokenText.length

        if (inQuotes) {
            val path = stripQuotes(tokenText).trim()
            if (path.isNotEmpty()) {
                val resolver = TemplateResolverService.getInstance(project)
                val templateTarget = resolver.resolveTemplatePath(path, file)
                if (templateTarget != null) return arrayOf(templateTarget)
            }
        }

        // 2. PHP/Variable navigation
        return resolveTwigPhpNavigation(leaf, offset, project, file)
    }

    private fun resolveTwigPhpNavigation(leaf: PsiElement, offset: Int, project: Project, file: PsiFile): Array<PsiElement>? {
        val text = leaf.text?.trim()?.removePrefix("$") ?: return null
        if (text.isEmpty() || text.length > 100) return null
        
        // Special handling for TwigMethodReference
        val parent = leaf.parent
        if (parent != null && parent.javaClass.simpleName == "TwigMethodReference") {
            // Parse the method reference: variable.method()
            val fullText = parent.text.trim()
            val match = Regex("""(\w+)\.(\w+)\s*\(""").find(fullText)
            if (match != null) {
                val varName = match.groupValues[1]
                val methodName = match.groupValues[2]
                
                val annotations = TwigPhpNavigationUtils.getVarAnnotations(file)
                val type = annotations[varName]
                if (type != null) {
                    val targets = TwigPhpNavigationUtils.resolveMethod(project, type, methodName)
                    if (targets.isNotEmpty()) {
                        return targets
                    }
                }
            }
        }
        
        // 1. Handle clicks inside comments (@var)
        val isComment = leaf.javaClass.simpleName.contains("Comment", ignoreCase = true) ||
                        parent?.javaClass?.simpleName?.contains("Comment", ignoreCase = true) == true
        
        if (isComment) {
            // Get comment text from leaf or parent
            val commentElement = if (leaf.javaClass.simpleName.contains("Comment", ignoreCase = true)) leaf else parent
            val commentText = commentElement?.text ?: return null
            
            // Calculate relative offset
            val relOffset = offset - (commentElement?.textRange?.startOffset ?: 0)
            
            // Match any non-whitespace for Type, and alphanumeric for name
            // Try both orders: @var name Type and @var Type name
            val patterns = listOf(
                Regex("""@var\s+\$?(\w+)\s+([^\s]+)"""),  // @var name Type
                Regex("""@var\s+([^\s]+)\s+\$?(\w+)""")   // @var Type name
            )
            
            for ((index, pattern) in patterns.withIndex()) {
                val matches = pattern.findAll(commentText)
                for (match in matches) {
                    val groupIndex = if (index == 0) 2 else 1  // Type is in group 2 for first pattern, group 1 for second
                    val typeNameGroup = match.groups[groupIndex]!!
                    val typeValue = typeNameGroup.value
                    
                    // Check if click is within the type name range
                    val typeStart = typeNameGroup.range.first
                    val typeEnd = typeNameGroup.range.last
                    
                    if (relOffset >= typeStart && relOffset <= typeEnd) {
                        val targets = TwigPhpNavigationUtils.resolveClass(project, typeValue)
                        if (targets.isNotEmpty()) {
                            return targets
                        }
                    }
                }
            }
        }

        // 2. Handle variable or method usage
        val annotations = TwigPhpNavigationUtils.getVarAnnotations(file)
        
        // Check if this is a known variable
        if (annotations.containsKey(text)) {
            val definition = TwigPhpNavigationUtils.findVarDefinition(file, text)
            if (definition != null) {
                return arrayOf(definition)
            }
        }
        
        // Try to find if this is a method call
        // Traverse up the tree and log everything
        var current: PsiElement? = leaf
        var depth = 0
        while (current != null && depth < 10) {
            val parent = current.parent ?: break
            val siblings = parent.children
            
            // Try to find pattern: variable . method
            for (i in 0 until siblings.size) {
                val child = siblings[i]
                
                // Check if this child is our element or contains it
                if (child == leaf || child == current || child.textContains(leaf.text.firstOrNull() ?: ' ')) {
                    // Look for a dot before us
                    if (i >= 1 && siblings[i-1].text.trim() == ".") {
                        // Look for variable before the dot
                        if (i >= 2) {
                            val varPart = siblings[i-2]
                            val varName = varPart.text.trim().removePrefix("$")
                            
                            val type = annotations[varName]
                            if (type != null) {
                                val targets = TwigPhpNavigationUtils.resolveMethod(project, type, text)
                                if (targets.isNotEmpty()) {
                                    return targets
                                }
                            }
                        }
                    }
                    
                    // Also check if we ARE the variable part (clicked on 'info' in 'info.run')
                    if (i + 2 < siblings.size && siblings[i+1].text.trim() == ".") {
                        val definition = TwigPhpNavigationUtils.findVarDefinition(file, text)
                        if (definition != null) {
                            return arrayOf(definition)
                        }
                    }
                }
            }
            
            current = parent
            depth++
        }
        
        return null
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String? = null

    private fun stripQuotes(s: String): String {
        if (s.length >= 2) {
            val first = s.first()
            val last = s.last()
            if ((first == '"' || first == '\'') && (last == '"' || last == '\'')) {
                return s.substring(1, s.length - 1)
            }
        }
        return s
    }

    private fun buildTokenText(element: PsiElement): String {
        // Try to include previous/next siblings if quotes are separate tokens
        val sb = StringBuilder()
        val prev = element.prevSibling
        if (prev != null && prev.text != null && (prev.text == "\"" || prev.text == "'")) {
            sb.append(prev.text)
        }
        sb.append(element.text)
        val next = element.nextSibling
        if (next != null && next.text != null && (next.text == "\"" || next.text == "'")) {
            sb.append(next.text)
        }
        return sb.toString()
    }
}
