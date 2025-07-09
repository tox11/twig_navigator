package com.twig5.references

import com.intellij.psi.PsiElement
import com.intellij.psi.ElementManipulators
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReferenceBase

/**
 * Reference implementation for PHP template path references.
 */
class PhpTwigTemplateReference(
    private val myElement: PsiElement,
    private val rangeInElement: TextRange,
    private val value: String
) : PsiReferenceBase<PsiElement>(myElement, rangeInElement) {

    override fun resolve(): PsiElement? {
        // Implementation for resolving the reference
        return null
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = myElement ?: throw IllegalStateException("Element should not be null")
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
        return manipulator.handleContentChange(element, rangeInElement, newPath) ?: element
    }

    override fun getVariants(): Array<Any> = emptyArray()
}