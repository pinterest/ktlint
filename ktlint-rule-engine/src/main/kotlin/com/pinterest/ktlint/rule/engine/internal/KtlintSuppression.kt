package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.findChildByTypeRecursively
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.replaceWith
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.util.prefixIfNot

private const val KTLINT_PREFIX = "ktlint"
private const val RULE_ID_SEPARATOR = ":"
private const val STANDARD_RULE_SET_PREFIX = "standard"
private const val EXPERIMENTAL_RULE_SET_PREFIX = "experimental"
internal const val KTLINT_SUPPRESSION_ID_ALL_RULES = "\"$KTLINT_PREFIX\""
private const val KTLINT_SUPPRESSION_ID_PREFIX = "$KTLINT_PREFIX$RULE_ID_SEPARATOR"
private const val DOUBLE_QUOTE = "\""

/**
 * Inserts or modifies the [Suppress] annotation on the given [ASTNode]. If no [Suppress] annotation may be created on the [ASTNode] then a
 * parent node will be targeted. If no parent node is found, the [Suppress] is added as "@file:Suppress" annotation. If the target node
 * already is annotated with [Suppress] then it is expanded with [suppressionIds] which are not yet listed in the annotation.
 */
internal fun ASTNode.insertKtlintRuleSuppression(
    suppressionIds: Set<String>,
    forceFileAnnotation: Boolean = false,
) {
    if (suppressionIds.isEmpty()) {
        // Do not add or alter the @Suppress / @SuppressWarnings
        return
    }
    val fullyQualifiedSuppressionIds = suppressionIds.map { it.toFullyQualifiedKtlintSuppressionId() }.toSet()

    val targetASTNode = this.findParentDeclarationOrExpression(forceFileAnnotation)
    val suppressionAnnotations = targetASTNode.findSuppressionAnnotations()
    // Add ktlint rule suppressions:
    //   - To the @Suppress annotation if found
    //   - otherwise to the @SuppressWarnings annotation if found
    //   - otherwise create a new @Suppress annotation
    when {
        suppressionAnnotations.containsKey(SuppressAnnotationType.SUPPRESS) -> {
            fullyQualifiedSuppressionIds.mergeInto(
                suppressionAnnotations.getValue(SuppressAnnotationType.SUPPRESS),
                SuppressAnnotationType.SUPPRESS,
            )
        }

        suppressionAnnotations.containsKey(SuppressAnnotationType.SUPPRESS_WARNINGS) -> {
            fullyQualifiedSuppressionIds.mergeInto(
                suppressionAnnotations.getValue(SuppressAnnotationType.SUPPRESS_WARNINGS),
                SuppressAnnotationType.SUPPRESS_WARNINGS,
            )
        }

        else -> {
            targetASTNode.createSuppressAnnotation(SuppressAnnotationType.SUPPRESS, fullyQualifiedSuppressionIds)
        }
    }
}

/**
 * Finds the node on which the [Suppress] may be added. This is either a declaration or an exception with some exceptions.
 */
private fun ASTNode.findParentDeclarationOrExpression(forceFileAnnotation: Boolean): ASTNode {
    if (!forceFileAnnotation && isTopLevel()) {
        return this
    }

    var targetPsiElement = psi
    var isAnnotationForBinaryExpression = false
    while (
        forceFileAnnotation ||
        targetPsiElement is KtClassInitializer ||
        targetPsiElement is KtBinaryExpression ||
        targetPsiElement is KtBlockExpression ||
        targetPsiElement is KtPrimaryConstructor ||
        targetPsiElement is KtFunctionLiteral ||
        targetPsiElement is KtLambdaExpression ||
        targetPsiElement.parent is KtStringTemplateExpression ||
        (
            targetPsiElement is KtExpression &&
                targetPsiElement.parent is KtExpression &&
                targetPsiElement.parent !is KtBlockExpression &&
                targetPsiElement.parent !is KtDeclaration
        ) ||
        (targetPsiElement !is KtDeclaration && targetPsiElement !is KtExpression)
    ) {
        if (targetPsiElement is KtBinaryExpression) {
            isAnnotationForBinaryExpression = true
        }
        targetPsiElement =
            when {
                targetPsiElement.parent == null -> {
                    // Prevents null pointer when already at a root node
                    return targetPsiElement.node
                }

                targetPsiElement.node.elementType in listElementTypeTokenSet && !isAnnotationForBinaryExpression -> {
                    // If a suppression is added to an inner element of a list element, then the annotation should be put on that element
                    // unless this is binary expression. Inserting the annotation on the root of the binary expression would result in
                    // applying that annotation only on the left hand side of the root expression instead of on the entire binary
                    // expression.
                    return targetPsiElement.node
                }

                targetPsiElement.isIgnorableListElement() -> {
                    // If a suppression is added on a direct child of the list type but not inside in a list element then the annotation is
                    // moved to the next list element. When no such element is found, it will be moved to the parent of the list type
                    targetPsiElement
                        .node
                        .nextCodeSibling()
                        ?.firstChildLeafOrSelf()
                        ?.psi
                }

                else -> {
                    targetPsiElement
                }
            }?.parent
    }
    return targetPsiElement.node
}

private val listTypeTokenSet = TokenSet.create(TYPE_ARGUMENT_LIST, TYPE_PARAMETER_LIST, VALUE_ARGUMENT_LIST, VALUE_PARAMETER_LIST)
private val listElementTypeTokenSet = TokenSet.create(TYPE_PROJECTION, TYPE_PARAMETER, VALUE_ARGUMENT, VALUE_PARAMETER)

private fun PsiElement.isIgnorableListElement() =
    node
        .takeIf { it.treeParent.elementType in listTypeTokenSet }
        ?.let { it.elementType == COMMA || it.isWhiteSpace() || it.isPartOfComment() }
        ?: false

/**
 * Determines whether a node is top level element
 */
internal fun ASTNode.isTopLevel() = this.elementType == FILE || this.treeParent.elementType == FILE

private fun Set<String>.mergeInto(
    annotationNode: ASTNode,
    suppressType: SuppressAnnotationType,
) {
    annotationNode
        .existingSuppressions()
        .plus(this)
        .let { suppressions ->
            if (suppressions.contains(KTLINT_SUPPRESSION_ID_ALL_RULES)) {
                // When all ktlint rules are to be suppressed, then ignore all suppressions for specific ktlint rules
                suppressions
                    .filterNot { it.isKtlintSuppressionId() }
                    .toSet()
            } else {
                suppressions
            }
        }.toSet()
        .let { suppressions -> annotationNode.createSuppressAnnotation(suppressType, suppressions) }
}

private fun ASTNode.existingSuppressions() =
    existingSuppressionsFromNamedArgumentOrNull()
        ?: getValueArguments()

private fun ASTNode.existingSuppressionsFromNamedArgumentOrNull(): Set<String>? =
    findChildByTypeRecursively(ElementType.COLLECTION_LITERAL_EXPRESSION, includeSelf = false)
        ?.run {
            children()
                .filter { it.elementType == ElementType.STRING_TEMPLATE }
                .map { it.text }
                .toSet()
        }

private fun ASTNode.findSuppressionAnnotations(): Map<SuppressAnnotationType, ASTNode> =
    if (this.isRoot()) {
        findChildByType(ElementType.FILE_ANNOTATION_LIST)
            ?.toMapOfSuppressionAnnotations()
            .orEmpty()
    } else if (this.elementType == ElementType.ANNOTATED_EXPRESSION) {
        this.toMapOfSuppressionAnnotations()
    } else {
        findChildByType(ElementType.MODIFIER_LIST)
            ?.toMapOfSuppressionAnnotations()
            .orEmpty()
    }

private fun ASTNode.toMapOfSuppressionAnnotations(): Map<SuppressAnnotationType, ASTNode> =
    children()
        .mapNotNull { modifier ->
            when (modifier.suppressionAnnotationTypeOrNull()) {
                SuppressAnnotationType.SUPPRESS -> Pair(SuppressAnnotationType.SUPPRESS, modifier)
                SuppressAnnotationType.SUPPRESS_WARNINGS -> Pair(SuppressAnnotationType.SUPPRESS_WARNINGS, modifier)
                else -> null
            }
        }.toMap()

private fun ASTNode.suppressionAnnotationTypeOrNull() =
    takeIf { elementType == ElementType.ANNOTATION || elementType == ElementType.ANNOTATION_ENTRY }
        ?.findChildByType(ElementType.CONSTRUCTOR_CALLEE)
        ?.findChildByType(ElementType.TYPE_REFERENCE)
        ?.findChildByType(ElementType.USER_TYPE)
        ?.findChildByType(ElementType.REFERENCE_EXPRESSION)
        ?.findChildByType(ElementType.IDENTIFIER)
        ?.text
        ?.let { SuppressAnnotationType.findByIdOrNull(it) }

private fun ASTNode.getValueArguments() =
    findChildByType(VALUE_ARGUMENT_LIST)
        ?.children()
        ?.filter { it.elementType == VALUE_ARGUMENT }
        ?.map { it.text }
        ?.toSet()
        .orEmpty()

private fun ASTNode.createSuppressAnnotation(
    suppressType: SuppressAnnotationType,
    suppressions: Set<String>,
) {
    val targetNode =
        if (elementType == ElementType.ANNOTATION_ENTRY) {
            treeParent
        } else {
            this
        }

    when (psi) {
        is KtFile -> {
            val fileAnnotation = targetNode.createFileAnnotation(suppressType, suppressions)
            this.createFileAnnotationList(fileAnnotation)
        }

        is KtAnnotationEntry -> {
            if (psi.parent is KtFileAnnotationList) {
                val fileAnnotation = targetNode.createFileAnnotation(suppressType, suppressions)
                this.replaceWith(fileAnnotation.firstChildNode)
            } else {
                val modifierListWithAnnotation = targetNode.createModifierListWithAnnotationEntry(suppressType, suppressions)
                this.replaceWith(
                    modifierListWithAnnotation
                        .getChildOfType<KtAnnotationEntry>()!!
                        .node,
                )
            }
        }

        is KtClass, is KtFunction, is KtProperty, is KtPropertyAccessor -> {
            this.addChild(PsiWhiteSpaceImpl(indent()), this.firstChildNode)
            val modifierListWithAnnotation = targetNode.createModifierListWithAnnotationEntry(suppressType, suppressions)
            this.addChild(modifierListWithAnnotation.node, this.firstChildNode)
        }

        else -> {
            if (targetNode.psi is KtExpression &&
                targetNode.psi !is KtAnnotatedExpression &&
                this.elementType != VALUE_PARAMETER
            ) {
                val annotatedExpression = targetNode.createAnnotatedExpression(suppressType, suppressions)
                treeParent.replaceChild(targetNode, annotatedExpression.node)
            } else {
                val modifierListWithAnnotation = targetNode.createModifierListWithAnnotationEntry(suppressType, suppressions)
                treeParent.addChild(
                    modifierListWithAnnotation
                        .getChildOfType<KtAnnotationEntry>()!!
                        .node,
                    this,
                )
                treeParent.addChild(PsiWhiteSpaceImpl(indent()), this)
            }
        }
    }
}

private fun ASTNode.createFileAnnotation(
    suppressType: SuppressAnnotationType,
    suppressions: Set<String>,
): ASTNode =
    suppressions
        .sorted()
        .joinToString()
        .let { sortedSuppressions -> "@file:${suppressType.annotationName}($sortedSuppressions)" }
        .let { annotation ->
            PsiFileFactory
                .getInstance(psi.project)
                .createFileFromText(KotlinLanguage.INSTANCE, annotation)
                ?.firstChild
                ?: throw IllegalStateException("Can not create annotation '$annotation'")
        }.node

private fun ASTNode.createFileAnnotationList(annotation: ASTNode) {
    require(isRoot()) { "File annotation list can only be created for root node" }
    // Should always be inserted into the first (root) code child regardless in which root node the ktlint directive
    // was actually found
    findChildByType(ElementType.PACKAGE_DIRECTIVE)
        ?.let { packageDirective ->
            packageDirective
                .treeParent
                .addChild(annotation, packageDirective)
            packageDirective
                .treeParent
                .addChild(PsiWhiteSpaceImpl("\n" + indent()), packageDirective)
        }
}

private fun ASTNode.createModifierListWithAnnotationEntry(
    suppressType: SuppressAnnotationType,
    suppressions: Set<String>,
): PsiElement =
    suppressions
        .sorted()
        .joinToString()
        .let { sortedSuppressions -> "@${suppressType.annotationName}($sortedSuppressions)" }
        .let { annotation ->
            PsiFileFactory
                .getInstance(psi.project)
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
                ?: throw IllegalStateException("Can not create annotation '$annotation'")
        }

private fun ASTNode.createAnnotatedExpression(
    suppressType: SuppressAnnotationType,
    suppressions: Set<String>,
): PsiElement =
    suppressions
        .sorted()
        .joinToString()
        .let { sortedSuppressions -> "@${suppressType.annotationName}($sortedSuppressions)" }
        .let { annotation ->
            PsiFileFactory
                .getInstance(psi.project)
                .createFileFromText(
                    KotlinLanguage.INSTANCE,
                    """
                    |${this.indent(false)}$annotation
                    |${this.indent(false)}${this.text}
                    """.trimMargin(),
                ).getChildOfType<KtScript>()
                ?.getChildOfType<KtBlockExpression>()
                ?.getChildOfType<KtScriptInitializer>()
                ?.getChildOfType<KtAnnotatedExpression>()
                ?: throw IllegalStateException("Can not create annotation '$annotation'")
        }

private enum class SuppressAnnotationType(
    val annotationName: String,
) {
    SUPPRESS("Suppress"),
    SUPPRESS_WARNINGS("SuppressWarnings"),
    ;

    companion object {
        fun findByIdOrNull(id: String): SuppressAnnotationType? = entries.firstOrNull { it.annotationName == id }
    }
}

internal fun String.isKtlintSuppressionId() = removePrefix(DOUBLE_QUOTE).startsWith(KTLINT_SUPPRESSION_ID_PREFIX)

internal fun String.toFullyQualifiedKtlintSuppressionId(): String =
    when (this) {
        KTLINT_SUPPRESSION_ID_ALL_RULES -> {
            this
        }

        KTLINT_PREFIX -> {
            this.surroundWith(DOUBLE_QUOTE)
        }

        else -> {
            removeSurrounding(DOUBLE_QUOTE)
                .qualifiedRuleIdString()
                .let { qualifiedRuleId -> "$KTLINT_PREFIX$RULE_ID_SEPARATOR$qualifiedRuleId" }
                .surroundWith(DOUBLE_QUOTE)
        }
    }

internal fun String.qualifiedRuleIdString() =
    removePrefix(KTLINT_SUPPRESSION_ID_PREFIX)
        .let { qualifiedSuppressionIdWithoutKtlintPrefix ->
            val ruleSetId =
                qualifiedSuppressionIdWithoutKtlintPrefix
                    .substringBefore(RULE_ID_SEPARATOR, STANDARD_RULE_SET_PREFIX)
                    .let {
                        if (it == EXPERIMENTAL_RULE_SET_PREFIX) {
                            // Historically the experimental rules were located in the experimental ruleset. References to that ruleset
                            // might still exist and can silently be replaced with the reference to the standard ruleset.
                            STANDARD_RULE_SET_PREFIX
                        } else {
                            it
                        }
                    }
            val ruleId = qualifiedSuppressionIdWithoutKtlintPrefix.substringAfter(RULE_ID_SEPARATOR)
            "$ruleSetId$RULE_ID_SEPARATOR$ruleId"
        }

private fun String.surroundWith(string: String) =
    removeSurrounding(string)
        .prefixIfNot(string)
        .plus(string)
