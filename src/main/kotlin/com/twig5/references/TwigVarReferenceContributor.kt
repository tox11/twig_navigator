package com.twig5.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.twig.TwigLanguage

/**
 * Provides references for Twig variables to PHP classes and methods via @var annotations.
 */
class TwigVarReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(TwigLanguage.INSTANCE),
            TwigVarReferenceProvider()
        )
    }
}

class TwigVarReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val text = element.text?.trim() ?: return PsiReference.EMPTY_ARRAY
        if (text.isEmpty() || text.length > 100) return PsiReference.EMPTY_ARRAY
        
        // Only process identifiers and variables
        val className = element.javaClass.simpleName
        if (!className.contains("Identifier", ignoreCase = true) && 
            !className.contains("Variable", ignoreCase = true)) {
            return PsiReference.EMPTY_ARRAY
        }
        
        return arrayOf(TwigVarReference(element, TextRange(0, text.length)))
    }
}

class TwigVarReference(
    element: PsiElement,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement, true) {
    
    override fun resolve(): PsiElement? {
        val element = element ?: return null
        val file = element.containingFile ?: return null
        val project = file.project
        val text = element.text?.trim()?.removePrefix("$") ?: return null
        
        val annotations = TwigPhpNavigationUtils.getVarAnnotations(file)
        
        // Check if we have a variable with this name
        if (annotations.containsKey(text)) {
            return TwigPhpNavigationUtils.findVarDefinition(file, text)
        }
        
        // Try to find if this is a method call on a variable
        // Look through parent hierarchy to find the pattern
        var current: PsiElement? = element
        var depth = 0
        while (current != null && depth < 10) {
            val parent = current.parent ?: break
            val siblings = parent.children
            
            // Look for pattern: something . identifier
            for (i in 0 until siblings.size) {
                val child = siblings[i]
                
                // Check if current element or its text matches this child
                if (child == element || child.text.trim() == text || child == current) {
                    // Look backwards for a dot and then a variable
                    if (i >= 2 && siblings[i-1].text.trim() == ".") {
                        val varPart = siblings[i-2]
                        val varName = varPart.text.trim().removePrefix("$")
                        
                        val type = annotations[varName]
                        if (type != null) {
                            val targets = TwigPhpNavigationUtils.resolveMethod(project, type, text)
                            if (targets.isNotEmpty()) return targets[0]
                        }
                    }
                }
            }
            
            current = parent
            depth++
        }
        
        return null
    }
    
    override fun getVariants(): Array<Any> = emptyArray()
}
