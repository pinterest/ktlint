package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Ensures that annotation are wrapped to separate lines.
 *
 * https://kotlinlang.org/docs/reference/coding-conventions.html#annotation-formatting
 *
 * 1) Place annotations on separate lines before the declaration to which they are attached, and with the same indentation:
 *
 *    ```
 *    @Target(AnnotationTarget.PROPERTY)
 *    annotation class JsonExclude
 *    ```
 *
 * 2) Annotations without arguments may be placed on the same line:
 *
 *    ```
 *    @JsonExclude @JvmField
 *    var x: String
 *    ```
 *
 * 3) A single annotation without arguments may be placed on the same line as the corresponding declaration:
 *
 *    ```
 *    @Test fun foo() { /*...*/ }
 *    ```
 *
 * 4) File annotations are placed after the file comment (if any), before the package statement, and are separated from package with a blank
 *    line (to emphasize the fact that they target the file and not the package).
 *
 * @see [AnnotationSpacingRule] for white space rules.
 */
public class AnnotationRule :
    StandardRule(
        id = "annotation",
        usesEditorConfigProperties = setOf(
            INDENT_SIZE_PROPERTY,
            INDENT_STYLE_PROPERTY,
        ),
    ) {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig = IndentConfig(
            indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
            tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
        )
        Unit
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            FILE_ANNOTATION_LIST -> {
                visitFileAnnotationList(node, emit, autoCorrect)
            }
            ANNOTATION -> {
                // Annotation array
                //     @[...]
                visitAnnotation(node, emit, autoCorrect)
            }
            ANNOTATION_ENTRY -> {
                visitAnnotationEntry(node, emit, autoCorrect)
            }
        }
    }

    private fun visitAnnotationEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == ANNOTATION_ENTRY)

        if (node.isAnnotationEntryWithValueArgumentList() &&
            node.treeParent.treeParent.elementType != VALUE_PARAMETER && // fun fn(@Ann("blah") a: String)
            node.treeParent.treeParent.elementType != VALUE_ARGUMENT && // fn(@Ann("blah") "42")
            !node.isPartOf(TYPE_ARGUMENT_LIST) && // val property: Map<@Ann("blah") String, Int>
            node.isNotReceiverTargetAnnotation()
        ) {
            checkForAnnotationWithParameterToBePlacedOnSeparateLine(node, emit, autoCorrect)
        }

        if (node.isOnSameLineAsAnnotatedConstruct()) {
            if (node.isPrecededByAnnotationOnAnotherLine()) {
                // Code below is disallowed
                //   @Foo1
                //   @Foo2 fun foo() {}
                emit(
                    node.startOffset,
                    "Annotation must be placed on a separate line when it is preceded by another annotation on a separate line",
                    true,
                )
                if (autoCorrect) {
                    node
                        .lastChildLeafOrSelf()
                        .nextLeaf()
                        ?.upsertWhitespaceBeforeMe(getNewlineWithIndent(node.treeParent))
                }
            }

            if (node.treeParent.elementType != ANNOTATION &&
                node.isPrecededByOtherAnnotationEntryOnTheSameLine() &&
                node.isLastAnnotationEntry()
            ) {
                // Code below is disallowed
                //   @Foo1 @Foo2 fun foo() {}
                // But following is allowed:
                //   @[Foo1 Foo2] fun foo() {}
                emit(
                    node.findAnnotatedConstruct().startOffset,
                    "Multiple annotations should not be placed on the same line as the annotated construct",
                    true,
                )
                if (autoCorrect) {
                    node
                        .lastChildLeafOrSelf()
                        .nextCodeLeaf()
                        ?.upsertWhitespaceBeforeMe(
                            getNewlineWithIndent(node.treeParent)
                                .applyIf(node.typeProjectionOrNull() != null) {
                                    plus(indentConfig.indent)
                                },
                        )
                }
            }

            node
                .typeProjectionOrNull()
                ?.let { typeProjection ->
                    // Code below is disallowed
                    //   var foo: List<@Foo1 @Foo2 String>
                    // But following is allowed:
                    //   var foo: List<@[Foo1 Foo2] String>
                    if (node.isFollowedByOtherAnnotationEntryOnTheSameLine() &&
                        node.isFirstAnnotationEntry()
                    ) {
                        emit(
                            typeProjection.startOffset,
                            "Annotations on a type reference should be placed on a separate line",
                            true,
                        )
                        if (autoCorrect) {
                            typeProjection
                                .upsertWhitespaceBeforeMe(
                                    getNewlineWithIndent(node.treeParent).plus(indentConfig.indent),
                                )
                        }
                    }
                    if (node.isPrecededByOtherAnnotationEntryOnTheSameLine() &&
                        node.isLastAnnotationEntry()
                    ) {
                        val annotatedConstruct = node.findAnnotatedConstruct()
                        emit(
                            annotatedConstruct.startOffset,
                            "Annotations on a type reference should be placed on a separate line",
                            true,
                        )
                        if (autoCorrect) {
                            annotatedConstruct
                                .nextLeaf { it.isCodeLeaf() && it.elementType != ElementType.COMMA }!!
                                .firstChildLeafOrSelf()
                                .upsertWhitespaceBeforeMe(getNewlineWithIndent(node.treeParent))
                        }
                    }
                }
        }

        if (node.isPrecededByOtherAnnotationEntryOnTheSameLine() && node.isPrecededByAnnotationOnAnotherLine()) {
            // Code below is disallowed
            //   @Foo1
            //   @Foo2 @Foo3
            //   fun foo() {}
            emit(
                node.startOffset,
                "All annotations should either be on a single line or all annotations should be on a separate line",
                true,
            )
            if (autoCorrect) {
                node
                    .firstChildLeafOrSelf()
                    .upsertWhitespaceBeforeMe(getNewlineWithIndent(node.treeParent))
            }
        }
    }

    private fun ASTNode.typeProjectionOrNull() =
        takeIf { elementType == ANNOTATION_ENTRY }
            ?.takeIf { it.treeParent.elementType == MODIFIER_LIST }
            ?.treeParent
            ?.takeIf { it.treeParent.elementType == TYPE_REFERENCE }
            ?.treeParent
            ?.takeIf { it.treeParent.elementType == TYPE_PROJECTION }
            ?.treeParent

    private fun ASTNode.isPrecededByAnnotationOnAnotherLine(): Boolean {
        val firstAnnotation = treeParent.findChildByType(ANNOTATION_ENTRY)
        return siblings(forward = false)
            .takeWhile { it != firstAnnotation }
            .any { it.isWhiteSpaceWithNewline() }
    }

    private fun checkForAnnotationWithParameterToBePlacedOnSeparateLine(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.isPrecededByOtherAnnotationEntry() && node.isOnSameLineAsPreviousAnnotationEntry()) {
            emit(
                node.startOffset,
                "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct",
                true,
            )
            if (autoCorrect) {
                node
                    .firstChildLeafOrSelf()
                    .upsertWhitespaceBeforeMe(" ")
            }
        }

        if (node.isOnSameLineAsNextAnnotationEntryOrAnnotatedConstruct()) {
            emit(
                node.startOffset,
                "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct",
                // Annotated expressions for which the annotation contains a parameter can be hard to correct
                // automatically. See examples below. For now, let them be fixed manually.
                // fun foo1() = @Suppress("DEPRECATION") bar()
                // if (@Suppress("DEPRECATION") bar()) { .. }
                node.treeParent.elementType != ANNOTATED_EXPRESSION,
            )
            if (autoCorrect) {
                node
                    .lastChildLeafOrSelf()
                    .nextLeaf()
                    .safeAs<LeafPsiElement>()
                    ?.let {
                        if (it.elementType == WHITE_SPACE) {
                            it.replaceWithText(getNewlineWithIndent(node.treeParent))
                        } else {
                            it.rawInsertBeforeMe(
                                PsiWhiteSpaceImpl(getNewlineWithIndent(node.treeParent)),
                            )
                        }
                    }
            }
        }
    }

    private fun ASTNode.isNotReceiverTargetAnnotation() = getAnnotationUseSiteTarget() != AnnotationUseSiteTarget.RECEIVER

    private fun ASTNode.getAnnotationUseSiteTarget() =
        psi
            .safeAs<KtAnnotationEntry>()
            ?.useSiteTarget
            ?.getAnnotationUseSiteTarget()

    private fun ASTNode.isAnnotationEntryWithValueArgumentList() = getAnnotationEntryValueArgumentList() != null

    private fun ASTNode.getAnnotationEntryValueArgumentList() =
        takeIf { it.elementType == ANNOTATION_ENTRY }
            ?.findChildByType(VALUE_ARGUMENT_LIST)

    private fun ASTNode.isFirstAnnotationEntry() =
        this ==
            treeParent
                .children()
                .firstOrNull { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isLastAnnotationEntry() =
        this ==
            treeParent
                .children()
                .lastOrNull { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isPrecededByOtherAnnotationEntryOnTheSameLine() =
        siblings(forward = false)
            .takeWhile { !it.isWhiteSpaceWithNewline() }
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isFollowedByOtherAnnotationEntryOnTheSameLine() =
        siblings()
            .takeWhile { !it.isWhiteSpaceWithNewline() }
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isPrecededByOtherAnnotationEntry() =
        siblings(forward = false)
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isOnSameLineAsPreviousAnnotationEntry() =
        siblings(forward = false)
            .takeWhile { it.elementType != ANNOTATION_ENTRY }
            .none { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.isFollowedByOtherAnnotationEntry() = siblings(forward = true).any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isOnSameLineAsNextAnnotationEntry() =
        siblings(forward = true)
            .takeWhile { it.elementType != ANNOTATION_ENTRY }
            .none { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.isOnSameLineAsAnnotatedConstruct(): Boolean {
        val annotatedConstruct = findAnnotatedConstruct()
        return lastChildLeafOrSelf()
            .leaves(forward = true)
            .takeWhile { it != annotatedConstruct }
            .none { it.isWhiteSpaceWithNewline() }
    }

    private fun ASTNode.findAnnotatedConstruct(): ASTNode {
        val astNode = if (treeParent.elementType == MODIFIER_LIST) {
            treeParent
        } else {
            this
        }

        return checkNotNull(
            astNode.lastChildLeafOrSelf().nextCodeLeaf(),
        )
    }

    private fun ASTNode.isOnSameLineAsNextAnnotationEntryOrAnnotatedConstruct() =
        if (isFollowedByOtherAnnotationEntry()) {
            isOnSameLineAsNextAnnotationEntry()
        } else {
            isOnSameLineAsAnnotatedConstruct()
        }

    private fun visitFileAnnotationList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .lastChildLeafOrSelf()
            .nextCodeLeaf()
            ?.let { codeLeaf ->
                val whitespaceBefore = codeLeaf.prevLeaf { it.isWhiteSpace() }

                if (whitespaceBefore == null || whitespaceBefore.text != "\n\n") {
                    emit(
                        codeLeaf.startOffset,
                        "File annotations should be separated from file contents with a blank line",
                        true,
                    )
                    if (autoCorrect) {
                        codeLeaf.upsertWhitespaceBeforeMe("\n\n")
                    }
                }
            }
    }

    private fun visitAnnotation(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == ANNOTATION)

        if ((node.isFollowedByOtherAnnotationEntry() && node.isOnSameLineAsNextAnnotationEntry()) ||
            (node.isPrecededByOtherAnnotationEntry() && node.isOnSameLineAsPreviousAnnotationEntry())
        ) {
            emit(
                node.startOffset,
                "@[...] style annotations should be on a separate line from other annotations.",
                true,
            )
            if (autoCorrect) {
                if (node.isFollowedByOtherAnnotationEntry()) {
                    node.upsertWhitespaceAfterMe(getNewlineWithIndent(node.treeParent))
                } else if (node.isPrecededByOtherAnnotationEntry()) {
                    node.upsertWhitespaceBeforeMe(getNewlineWithIndent(node.treeParent))
                }
            }
        }
    }

    private fun getNewlineWithIndent(modifierListRoot: ASTNode): String {
        val nodeBeforeAnnotations = modifierListRoot.treeParent.treePrev as? PsiWhiteSpace
        // If there is no whitespace before the annotation, the annotation is the first
        // text in the file
        val newLineWithIndent = nodeBeforeAnnotations?.text ?: "\n"
        return if (newLineWithIndent.contains('\n')) {
            // Make sure we only insert a single newline
            newLineWithIndent.substring(newLineWithIndent.lastIndexOf('\n'))
        } else {
            newLineWithIndent
        }
    }
}

public val ANNOTATION_RULE_ID: RuleId = AnnotationRule().ruleId
