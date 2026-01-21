package com.twig5.references

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass

object TwigPhpNavigationUtils {
    private val VAR_ANNOTATION_PATTERN = Regex("""@var\s+\$?(\w+)\s+([\\\\\w./-]+)""")

    /**
     * Finds all @var annotations in the file.
     * Returns a map of variable name to class FQN.
     */
    fun getVarAnnotations(file: PsiFile): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val comments = PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java)
        val allElements = mutableListOf<PsiElement>()
        allElements.addAll(comments)
        
        file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.javaClass.simpleName.contains("Comment", ignoreCase = true) && element !in comments) {
                    allElements.add(element)
                }
                super.visitElement(element)
            }
        })

        for (comment in allElements) {
            val text = comment.text ?: continue
            Regex("""@var\s+([^\s]+)\s+\$?(\w+)""").findAll(text).forEach { match ->
                val type = match.groupValues[1].trim()
                val name = match.groupValues[2].trim()
                result[name] = type
            }
            Regex("""@var\s+\$?(\w+)\s+([^\s]+)""").findAll(text).forEach { match ->
                val name = match.groupValues[1].trim()
                val type = match.groupValues[2].trim()
                result[name] = type
            }
        }
        return result
    }

    fun findVarDefinition(file: PsiFile, varName: String): PsiElement? {
        val annotations = getVarAnnotations(file)
        if (!annotations.containsKey(varName)) {
             return null
        }
        
        var found: PsiElement? = null
        file.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (found != null) return
                if (element.javaClass.simpleName.contains("Comment", ignoreCase = true)) {
                    val text = element.text ?: return
                    if (text.contains("@var") && (text.contains(" $varName ") || text.contains(" \$$varName ") || text.contains("\$$varName") || text.contains("@var $varName") || text.contains("@var \$$varName"))) {
                        found = element
                        return
                    }
                }
                super.visitElement(element)
            }
        })
        return found
    }

    fun resolveClass(project: Project, className: String): Array<PsiElement> {
        val phpIndex = PhpIndex.getInstance(project)
        
        var cleanName = className.trim()
            .replace('/', '\\')
            .substringBeforeLast(".php")
            .trim()
        
        val variants = mutableListOf<String>()
        variants.add(cleanName)
        
        if (!cleanName.startsWith("\\")) {
            variants.add("\\$cleanName")
        }
        
        if (cleanName.startsWith("\\")) {
            variants.add(cleanName.substring(1))
        }
        
        if (cleanName.contains("application")) {
            val withoutApp = cleanName.replace("application\\", "").replace("\\application\\", "\\")
            variants.add(withoutApp)
            if (!withoutApp.startsWith("\\")) {
                variants.add("\\$withoutApp")
            }
        }
        
        for (variant in variants) {
            val targets = phpIndex.getAnyByFQN(variant)
            if (targets.isNotEmpty()) {
                return targets.toTypedArray() as Array<PsiElement>
            }
        }

        val lastPart = cleanName.substringAfterLast("\\")
        if (lastPart.isNotEmpty() && lastPart != cleanName) {
            val targets = phpIndex.getClassesByName(lastPart)
            if (targets.isNotEmpty()) {
                return targets.toTypedArray() as Array<PsiElement>
            }
        }

        return emptyArray()
    }

    fun resolveMethod(project: Project, className: String, methodName: String): Array<PsiElement> {
        val cleanMethodName = methodName.substringBefore('(').trim()
        val classes = resolveClass(project, className)
        val targets = mutableListOf<PsiElement>()
        for (element in classes) {
            if (element is PhpClass) {
                val m = element.findMethodByName(cleanMethodName)
                if (m != null) {
                    targets.add(m)
                }
            }
        }
        return targets.toTypedArray()
    }
}
