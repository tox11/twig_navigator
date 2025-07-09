package com.twig5.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PatternCondition
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.twig.TwigLanguage
import com.jetbrains.twig.elements.TwigElementTypes
import com.jetbrains.twig.elements.TwigTag
import com.twig5.services.TemplateResolverService

/**
 * Provides references from Twig templates (e.g., {% include %}) to other Twig files.
 */
class TwigTemplateReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withParent(
                    PlatformPatterns.psiElement()
                        .with(object : PatternCondition<PsiElement>("TwigTagWithName") {
                            override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                                return t is TwigTag && t.getTagName() in setOf("include", "embed", "extends", "from", "import")
                            }
                        })
                )
                .withLanguage(TwigLanguage.INSTANCE),
            TwigTemplateReferenceProvider()
        )
    }
}

/**
 * Provides the actual reference for a string literal inside a Twig tag.
 */
class TwigTemplateReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // The element is the STRING_LITERAL.
        // We create a reference for its content.
        val textRange = ElementManipulators.getValueTextRange(element)
        return arrayOf(TwigTemplateReference(element, textRange))
    }
}

/**
 * The actual reference implementation for Twig-to-Twig navigation.
 */
class TwigTemplateReference(
    element: PsiElement,
    textRange: TextRange
) : PsiReferenceBase<PsiElement>(element, textRange, true) {

    override fun resolve(): PsiElement? {
        val element = myElement ?: return null
        val project = element.project
        val resolver = TemplateResolverService.getInstance(project)
        return resolver.resolveTemplatePath(value, element.containingFile)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = myElement ?: throw IllegalStateException("Element should not be null")
        val manipulator = ElementManipulators.getManipulator(element) ?: return element
        return manipulator.handleContentChange(element, rangeInElement, newElementName) ?: element
    }

    override fun getVariants(): Array<Any> = emptyArray()
}