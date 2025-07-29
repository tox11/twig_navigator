package com.twig5.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.*
import com.twig5.services.TemplateResolverService

/**
 * Provides references from PHP files to Twig templates in method calls like render() and display().
 */
class PhpTemplatePathReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Match string literals in method calls like $this->render() or $this->get('templating')->render()
        val pattern = PlatformPatterns.psiElement(StringLiteralExpression::class.java)
            .withParent(
                PlatformPatterns.psiElement(ParameterList::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(MethodReference::class.java)
                            .with(object : com.intellij.patterns.PatternCondition<MethodReference>("isTemplateRenderingMethod") {
                                override fun accepts(methodRef: MethodReference, context: ProcessingContext?): Boolean {
                                    return isTemplateRenderingMethod(methodRef)
                                }
                            })
                    )
            )

        registrar.registerReferenceProvider(
            pattern,
            PhpTemplateReferenceProvider()
        )
    }

    private fun isTemplateRenderingMethod(methodRef: MethodReference): Boolean {
        val methodName = methodRef.name ?: return false
        return methodName in RENDERING_METHODS
    }

    companion object {
        private val RENDERING_METHODS = setOf(
            "render",
            "display",
            "renderView",
            "renderTemplate",
            "createTemplate"
        )
    }
}

/**
 * Provides the actual reference for template paths in PHP method calls.
 */
class PhpTemplateReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is StringLiteralExpression) return PsiReference.EMPTY_ARRAY
        
        val manipulator = ElementManipulators.getManipulator(element) ?: return PsiReference.EMPTY_ARRAY
        val textRange = manipulator.getRangeInElement(element)
        if (textRange.isEmpty) return PsiReference.EMPTY_ARRAY
        
        // Get the template path without quotes
        val templatePath = element.contents
        if (templatePath.isBlank()) return PsiReference.EMPTY_ARRAY
        
        return arrayOf(PhpTwigTemplateReference(element, textRange, templatePath))
    }
}

/**
 * Reference implementation for PHP template path references.
 */
class PhpTwigTemplateReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val value: String
) : PsiReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun resolve(): PsiElement? {
        val element = element ?: return null
        val project = element.project
        val resolver = TemplateResolverService.getInstance(project)
        return resolver.resolveTemplatePath(value, element.containingFile)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = element ?: throw IllegalStateException("Element should not be null")
        val manipulator = ElementManipulators.getManipulator(element) ?: return element

        // When a Twig file is renamed, newElementName is the new file name (e.g., "new-name.twig").
        // We typically want the path without the extension in the PHP code.
        val newNameWithoutExtension = newElementName.substringBeforeLast('.')
        val oldPath = value
        val directoryPath = oldPath.substringBeforeLast("/", "")
        val newPath = if (directoryPath.isNotEmpty()) {
            "$directoryPath/$newNameWithoutExtension"
        } else {
            newNameWithoutExtension
        }
        
        // Create a new range that includes the quotes
        val rangeWithQuotes = TextRange(
            rangeInElement.startOffset - 1,
            rangeInElement.endOffset + 1
        )
        
        return manipulator.handleContentChange(element, rangeWithQuotes, "'$newPath'") ?: element
    }

    override fun getVariants(): Array<Any> = emptyArray()
}