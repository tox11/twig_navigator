package com.twig5.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PatternCondition
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.twig.TwigLanguage
import com.twig5.services.TemplateResolverService

/**
 * Provides references from Twig templates (e.g., {% include %}) to other Twig files.
 */
class TwigTemplateReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Use raw string and \s to avoid unsupported escapes
        val includeLike = Regex("""\{\%\s*(include|embed|extends|from|import)\b""", RegexOption.IGNORE_CASE)

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(TwigLanguage.INSTANCE)
                .with(object : PatternCondition<PsiElement>("TwigIncludeStringCompat") {
                    override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                        // Walk up a few levels to find a Twig tag text that matches include-like pattern
                        var p: PsiElement? = t
                        var depth = 0
                        var inIncludeTag = false
                        while (p != null && depth < 6) {
                            val txt = p.text
                            if (txt.contains("{%") && includeLike.containsMatchIn(txt)) {
                                inIncludeTag = true
                                break
                            }
                            p = p.parent
                            depth++
                        }
                        return inIncludeTag && t.textLength > 0
                    }
                }),
            TwigTemplateReferenceProvider()
        )
    }
}

/**
 * Provides the actual reference for a string literal inside a Twig tag.
 */
class TwigTemplateReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val text = element.text
        if (text.isBlank()) return PsiReference.EMPTY_ARRAY

        // Compute inner range excluding surrounding quotes if present
        val startsWithQuote = text.firstOrNull() == '\'' || text.firstOrNull() == '"'
        val endsWithQuote = text.lastOrNull() == '\'' || text.lastOrNull() == '"'
        val start = if (startsWithQuote) 1 else 0
        val endExclusiveRaw = text.length - if (endsWithQuote && text.length > 1) 1 else 0
        if (endExclusiveRaw <= start) return PsiReference.EMPTY_ARRAY
        val textRange = TextRange(start, endExclusiveRaw)

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