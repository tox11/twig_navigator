package com.twig5.references

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.twig.TwigLanguage
import com.twig5.services.TemplateResolverService

class TwigGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        if (element == null) return null
        val file = element.containingFile ?: return null
        val project: Project = file.project

        // Only act in Twig files
        if (file.language !is TwigLanguage) return null

        // Find the leaf at caret
        val leaf = file.findElementAt(offset) ?: return null

        // Extract potential quoted string around caret
        val text = leaf.text
        if (text.isNullOrEmpty()) return null

        // Build the full string token text possibly from siblings (Twig may split quotes and content)
        val tokenText = buildTokenText(leaf)
        val relativeOffset = offset - leaf.textRange.startOffset
        val inQuotes = relativeOffset >= 0 && relativeOffset <= tokenText.length

        if (!inQuotes) return null

        val path = stripQuotes(tokenText).trim()
        if (path.isEmpty()) return null

        val resolver = TemplateResolverService.getInstance(project)
        val target = resolver.resolveTemplatePath(path, file) ?: return null
        return arrayOf(target)
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
