package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.KtlintSuppressionRule.SuppressAnnotationType.SUPPRESS
import com.pinterest.ktlint.ruleset.standard.rules.KtlintSuppressionRule.SuppressAnnotationType.SUPPRESS_WARNINGS
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.util.prefixIfNot

/**
 * Disallow usage of the old "ktlint-disable" and "ktlint-enable" directives.
 *
 * A ktlint-disable directive is replaced with an annotation on the closest parent declaration or expression, or as annotation on the file
 * level in case the directive is associated with a top level element.
 *
 * If the target element is annotated with a [Suppress], or if missing with a [SuppressWarnings] annotation then the ktlint-directive will
 * be matched against this annotation. If this annotation already contains a suppression for *all* ktlint rules, or for the specific rule
 * id, then it is not added to annotation as it would be redundant. In case a suppression identifier is added to an existing annotation then
 * all identifiers in the annotation are alphabetically sorted.
 *
 * If the target element is not annotated with [Suppress] or [SuppressWarnings] then a [Suppress] annotation is added.
 *
 * A ktlint-enable directive is removed as annotations have a scope in which the suppression will be active.
 */
public class KtlintSuppressionRule : StandardRule("ktlint-suppression") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .suppressionAnnotationTypeOrNull()
            ?.visitSuppressAnnotation(node, emit, autoCorrect)

        node
            .removeCommentWithoutMarkersOrNull()
            ?.let { commentWithoutMarkers ->
                when {
                    commentWithoutMarkers.startsWith(KTLINT_DISABLE) -> {
                        if (node.elementType == EOL_COMMENT && node.prevLeaf().isWhiteSpaceWithNewline()) {
                            removeDanglingEolCommentWithKtlintDisableDirective(node, autoCorrect, emit)
                        } else {
                            visitKtlintDisableDirective(node, autoCorrect, emit, commentWithoutMarkers)
                        }
                    }

                    commentWithoutMarkers.startsWith(KTLINT_ENABLE) -> {
                        removeKtlintEnableDirective(node, autoCorrect, emit)
                    }

                    else -> null
                }
            }
    }

    private fun SuppressAnnotationType.visitSuppressAnnotation(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .findChildByType(VALUE_ARGUMENT_LIST)
            ?.let { valueArgumentList ->
                val suppressions =
                    valueArgumentList
                        .children()
                        .filter { it.elementType == VALUE_ARGUMENT }
                        .map { valueArgument ->
                            valueArgument
                                .text
                                .prefixKtlintSuppressionWithRuleSetId()
                                .also { prefixedSuppression ->
                                    if (prefixedSuppression != valueArgument.text) {
                                        emit(
                                            valueArgument.startOffset + "\"ktlint:".length,
                                            "Identifier to suppress ktlint rule must be fully qualified with the rule set id",
                                            true,
                                        )
                                        // See below for autocorrect on entire value argument list
                                    }
                                }
                        }.toSet()
                if (autoCorrect) {
                    node
                        .psi
                        .createSuppressAnnotation(this, suppressions)
                        .node
                        .findChildByType(VALUE_ARGUMENT_LIST)
                        ?.let { newValueArgumentList ->
                            if (valueArgumentList != newValueArgumentList) {
                                valueArgumentList.replaceWith(newValueArgumentList)
                            }
                        }
                }
            }
    }

    private fun String.prefixKtlintSuppressionWithRuleSetId() =
        takeIf { startsWith("\"ktlint:") }
            ?.substringAfter("\"ktlint:")
            ?.let { originalRuleId ->
                RuleId
                    .prefixWithStandardRuleSetIdWhenMissing(originalRuleId)
                    .prefixIfNot("\"ktlint:")
            }
            ?: this

    private fun ASTNode.removeCommentWithoutMarkersOrNull() =
        when (elementType) {
            EOL_COMMENT ->
                text
                    .removePrefix("//")
                    .trim()

            BLOCK_COMMENT ->
                text
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .trim()

            else -> null
        }

    private fun removeDanglingEolCommentWithKtlintDisableDirective(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(
            node.ktlintDisableOffset(),
            "Directive 'ktlint-disable' in EOL comment is ignored as it is not preceded by a code element",
            true,
        )
        if (autoCorrect) {
            node
                .prevLeaf()
                .takeIf { it.isWhiteSpace() }
                ?.remove()
            node.remove()
        }
    }

    private fun ASTNode.ktlintDisableOffset() = startOffset + text.indexOf(KTLINT_DISABLE)

    private fun visitKtlintDisableDirective(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ktlintDisableDirective: String,
    ) {
        emit(
            node.ktlintDisableOffset(),
            "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation",
            true,
        )
        if (autoCorrect) {
            node
                .findParentDeclarationOrExpression()
                .addKtlintRuleSuppression(ktlintDisableDirective.toFullyQualifiedSuppressKtlintRuleIds())
            if (node.elementType == EOL_COMMENT) {
                node
                    .prevLeaf()
                    .takeIf { it.isWhiteSpace() }
                    ?.remove()
            } else {
                if (node.nextLeaf().isWhiteSpaceWithNewline()) {
                    node
                        .nextLeaf()
                        ?.remove()
                } else {
                    node
                        .prevLeaf()
                        .takeIf { it.isWhiteSpace() }
                        ?.remove()
                }
            }
            node.remove()
        }
    }

    private fun ASTNode.findParentDeclarationOrExpression(): ASTNode {
        var targetNode = this
        while (
            !targetNode.isRoot() &&
            targetNode.psi !is KtDeclaration &&
            (targetNode.psi !is KtExpression || targetNode.psi is KtBlockExpression)
        ) {
            targetNode = targetNode.treeParent
        }
        return targetNode
    }

    private fun ASTNode.remove() {
        treeParent.removeChild(this)
    }

    private fun ASTNode.addKtlintRuleSuppression(ktlintRuleSuppressions: Set<String>) {
        val suppressionAnnotations = findSuppressionAnnotations()
        // Add ktlint rule suppressions:
        //   - To the @Suppress annotation if found
        //   - otherwise to the @SuppressWarnings annotation if found
        //   - otherwise create a new @Suppress annotation
        when {
            suppressionAnnotations.containsKey(SUPPRESS) ->
                ktlintRuleSuppressions.mergeInto(suppressionAnnotations.getValue(SUPPRESS), SUPPRESS)

            suppressionAnnotations.containsKey(SUPPRESS_WARNINGS) ->
                ktlintRuleSuppressions.mergeInto(suppressionAnnotations.getValue(SUPPRESS_WARNINGS), SUPPRESS_WARNINGS)

            else -> {
                psi
                    .createSuppressAnnotation(SUPPRESS, ktlintRuleSuppressions)
                    .node
                    .let { annotation ->
                        if (annotation.elementType == FILE_ANNOTATION_LIST) {
                            require(isRoot()) { "File annotation list can only be created for root node" }
                            // Should always be inserted into the first (root) code child regardless in which root node the ktlint directive
                            // was actually found
                            if (findChildByType(FILE_ANNOTATION_LIST) != null) {
                                findChildByType(FILE_ANNOTATION_LIST)
                                    ?.let { fileAnnotationList ->
                                        fileAnnotationList.treeParent.addChild(annotation, fileAnnotationList)
                                        fileAnnotationList.remove()
                                    }
                            } else {
                                findChildByType(PACKAGE_DIRECTIVE)
                                    ?.let { packageDirective ->
                                        packageDirective
                                            .treeParent
                                            .addChild(annotation, packageDirective)
                                        packageDirective
                                            .treeParent
                                            .addChild(PsiWhiteSpaceImpl("\n" + indent()), packageDirective)
                                    }
                            }
                        } else {
                            treeParent.addChild(annotation, this)
                            treeParent.addChild(PsiWhiteSpaceImpl(indent()), this)
                        }
                    }
            }
        }
    }

    private fun Set<String>.mergeInto(
        annotationNode: ASTNode,
        suppressType: SuppressAnnotationType,
    ) {
        annotationNode
            .getExistingSuppressions()
            .plus(this)
            .let { suppressions ->
                if (suppressions.contains("\"ktlint\"")) {
                    // When all ktlint rules are to be suppressed, then ignore all suppressions for specific ktlint rules
                    suppressions
                        .filterNot { it.startsWith("\"ktlint:") }
                        .toSet()
                } else {
                    suppressions
                }
            }.map { it.prefixKtlintSuppressionWithRuleSetId() }
            .let { suppressions ->
                annotationNode
                    .treeParent
                    .psi
                    .createSuppressAnnotation(suppressType, suppressions)
                    .node
                    .let { newAnnotation ->
                        if (annotationNode.treeParent.elementType == FILE_ANNOTATION_LIST) {
                            annotationNode.treeParent.replaceWith(newAnnotation)
                        } else {
                            annotationNode.replaceWith(newAnnotation)
                        }
                    }
            }
    }

    private fun ASTNode.findSuppressionAnnotations(): Map<SuppressAnnotationType, ASTNode> =
        if (this.isRoot()) {
            findChildByType(FILE_ANNOTATION_LIST)
                ?.findSuppressionAnnotationsInModifierList()
                .orEmpty()
        } else {
            findChildByType(MODIFIER_LIST)
                ?.findSuppressionAnnotationsInModifierList()
                .orEmpty()
        }

    private fun ASTNode.findSuppressionAnnotationsInModifierList(): Map<SuppressAnnotationType, ASTNode> =
        children()
            .mapNotNull { modifier ->
                when (modifier.suppressionAnnotationTypeOrNull()) {
                    SUPPRESS -> Pair(SUPPRESS, modifier)
                    SUPPRESS_WARNINGS -> Pair(SUPPRESS_WARNINGS, modifier)
                    else -> null
                }
            }.toMap()

    private fun ASTNode.suppressionAnnotationTypeOrNull() =
        takeIf { elementType == ANNOTATION || elementType == ANNOTATION_ENTRY }
            ?.findChildByType(ElementType.CONSTRUCTOR_CALLEE)
            ?.findChildByType(ElementType.TYPE_REFERENCE)
            ?.findChildByType(ElementType.USER_TYPE)
            ?.findChildByType(ElementType.REFERENCE_EXPRESSION)
            ?.findChildByType(ElementType.IDENTIFIER)
            ?.text
            ?.let { SuppressAnnotationType.findByIdOrNull(it) }

    private fun ASTNode.getExistingSuppressions() =
        findChildByType(VALUE_ARGUMENT_LIST)
            ?.children()
            ?.filter { it.elementType == VALUE_ARGUMENT }
            ?.map { it.text }
            ?.toSet()
            .orEmpty()

    private fun PsiElement.createSuppressAnnotation(
        suppressType: SuppressAnnotationType,
        suppressions: Collection<String>,
    ): PsiElement =
        suppressions
            .sorted()
            .joinToString()
            .let { sortedSuppressionsString ->
                if (this is KtFile || this is KtFileAnnotationList) {
                    createFileAnnotation(suppressType, sortedSuppressionsString)
                } else {
                    createDeclarationAnnotationEntry(suppressType, sortedSuppressionsString)
                }
            }

    private fun PsiElement.createFileAnnotation(
        suppressType: SuppressAnnotationType,
        sortedSuppressionsString: String,
    ): PsiElement =
        "@file:${suppressType.annotationName}($sortedSuppressionsString)"
            .let { annotation ->
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(KotlinLanguage.INSTANCE, annotation)
                    ?.firstChild
                    ?: throw IllegalStateException("Can not create annotation '$annotation'")
            }

    private fun PsiElement.createDeclarationAnnotationEntry(
        suppressType: SuppressAnnotationType,
        sortedSuppressionsString: String,
    ): PsiElement =
        "@${suppressType.annotationName}($sortedSuppressionsString)"
            .let { annotation ->
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(
                        KotlinLanguage.INSTANCE,
                        // Create the annotation for a dummy declaration as the entire code block should be valid Kotlin code
                        """
                        $annotation
                        fun foo() {}
                        """.trimIndent(),
                    ).getChildOfType<KtScript>()
                    ?.getChildOfType<KtBlockExpression>()
                    ?.getChildOfType<KtNamedFunction>()
                    ?.getChildOfType<KtDeclarationModifierList>()
                    ?.getChildOfType<KtAnnotationEntry>()
                    ?: throw IllegalStateException("Can not create annotation '$annotation'")
            }

    private fun String.toFullyQualifiedSuppressKtlintRuleIds(): Set<String> =
        removePrefix(KTLINT_DISABLE)
            .trim()
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map {
                it.prefixKtlintSuppressionWithRuleSetId()
                RuleId
                    .prefixWithStandardRuleSetIdWhenMissing(it)
                    .prefixIfNot("ktlint:")
            }.ifEmpty { listOf("ktlint") }
            .map { it.surroundWith('"') }
            .toSet()

    private fun String.surroundWith(char: Char) =
        char
            .toString()
            .let { string ->
                removeSurrounding(string)
                    .prefixIfNot(string)
                    .plus(string)
            }

    private fun removeKtlintEnableDirective(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(
            node.startOffset + node.text.indexOf(KTLINT_ENABLE),
            "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations",
            true,
        )
        if (autoCorrect) {
            node
                .prevLeaf()
                .takeIf { it.isWhiteSpace() }
                ?.remove()
            node.remove()
        }
    }

    private fun ASTNode.replaceWith(node: ASTNode) {
        treeParent.addChild(node, this)
        this.remove()
    }

    private enum class SuppressAnnotationType(val annotationName: String) {
        SUPPRESS("Suppress"),
        SUPPRESS_WARNINGS("SuppressWarnings"),
        ;

        companion object {
            fun findByIdOrNull(id: String): SuppressAnnotationType? =
                SuppressAnnotationType
                    .values()
                    .firstOrNull { it.annotationName == id }
        }
    }

    private companion object {
        const val KTLINT_DISABLE = "ktlint-disable"
        const val KTLINT_ENABLE = "ktlint-enable"
    }
}

public val KTLINT_SUPPRESSION_RULE_ID: RuleId = KtlintSuppressionRule().ruleId
