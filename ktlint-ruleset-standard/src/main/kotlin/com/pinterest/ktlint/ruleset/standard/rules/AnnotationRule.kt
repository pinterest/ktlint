package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

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
@SinceKtlint("0.30", STABLE)
public class AnnotationRule :
    StandardRule(
        id = "annotation",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
        visitorModifiers =
            setOf(
                RunAfterRule(
                    ruleId = ENUM_WRAPPING_RULE_ID,
                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                ),
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            FILE_ANNOTATION_LIST -> {
                visitAnnotationList(node, emit, autoCorrect)
                visitFileAnnotationList(node, emit, autoCorrect)
            }
            ANNOTATED_EXPRESSION, MODIFIER_LIST -> {
                visitAnnotationList(node, emit, autoCorrect)
            }
            ANNOTATION -> {
                // Annotation array
                //     @[...]
                visitAnnotation(node, emit, autoCorrect)
            }
            ANNOTATION_ENTRY -> {
                visitAnnotationEntry(node, emit, autoCorrect)
            }
            TYPE_ARGUMENT_LIST -> {
                visitTypeArgumentList(node, emit, autoCorrect)
            }
        }
    }

    private fun visitAnnotationList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType in ANNOTATION_CONTAINER)

        if (node.shouldWrapAnnotations()) {
            val expectedIndent =
                when {
                    node.elementType == ANNOTATED_EXPRESSION ->
                        indentConfig.siblingIndentOf(node)
                    node.hasAnnotationBeforeConstructor() ->
                        indentConfig.siblingIndentOf(node.treeParent)
                    else ->
                        indentConfig.parentIndentOf(node)
                }

            node
                .children()
                .filter { it.elementType == ANNOTATION_ENTRY }
                .filter {
                    it.isAnnotationEntryWithValueArgumentList() ||
                        !it.isPrecededByOtherAnnotationEntryWithoutParametersOnTheSameLine()
                }.forEachIndexed { index, annotationEntry ->
                    annotationEntry
                        .prevLeaf()
                        .takeUnless {
                            // Allow in ktlint_official code style:
                            //     class Foo(
                            //         bar: Bar,
                            //     ) : @Suppress("DEPRECATION")
                            //         FooBar()
                            index == 0 &&
                                codeStyle == CodeStyleValue.ktlint_official &&
                                it.annotationOnSameLineAsClosingParenthesisOfClassParameterList()
                        }?.let { prevLeaf ->
                            // Let the indentation rule determine the exact indentation and only report and fix when the line needs to be
                            // wrapped
                            if (!prevLeaf.textContains('\n')) {
                                emit(prevLeaf.startOffset, "Expected newline before annotation", true)
                                if (autoCorrect) {
                                    prevLeaf.upsertWhitespaceBeforeMe(
                                        prevLeaf
                                            .text
                                            .substringBeforeLast('\n', "")
                                            .plus(expectedIndent),
                                    )
                                }
                            }
                        }
                }

            node
                .children()
                .last { it.elementType == ANNOTATION_ENTRY }
                .lastChildLeafOrSelf()
                .nextCodeLeaf()
                ?.prevLeaf()
                ?.let { prevLeaf ->
                    // Let the indentation rule determine the exact indentation and only report and fix when the line needs to be wrapped
                    if (!prevLeaf.textContains('\n')) {
                        emit(prevLeaf.startOffset, "Expected newline after last annotation", true)
                        if (autoCorrect) {
                            prevLeaf.upsertWhitespaceAfterMe(expectedIndent)
                        }
                    }
                }

            node
                .takeIf { it.elementType == ANNOTATED_EXPRESSION }
                ?.lastChildLeafOrSelf()
                ?.nextCodeLeaf()
                ?.prevLeaf()
                ?.let { leaf ->
                    // Let the indentation rule determine the exact indentation and only report and fix when the line needs to be wrapped
                    if (!leaf.textContains('\n')) {
                        emit(leaf.startOffset, "Expected newline", true)
                        if (autoCorrect) {
                            leaf.upsertWhitespaceBeforeMe(node.indent())
                        }
                    }
                }
        }
    }

    private fun ASTNode.shouldWrapAnnotations() =
        hasAnnotationWithParameter() ||
            hasMultipleAnnotationsOnSameLine() ||
            hasAnnotationBeforeConstructor()

    private fun ASTNode.hasAnnotationWithParameter(): Boolean {
        require(elementType in ANNOTATION_CONTAINER)
        return children()
            .any {
                it.isAnnotationEntryWithValueArgumentList() &&
                    it.treeParent.treeParent.elementType != VALUE_PARAMETER &&
                    it.treeParent.treeParent.elementType != VALUE_ARGUMENT &&
                    it.isNotReceiverTargetAnnotation()
            }
    }

    private fun ASTNode.hasMultipleAnnotationsOnSameLine(): Boolean {
        require(elementType in ANNOTATION_CONTAINER)
        return children()
            .any {
                it.treeParent.elementType != ANNOTATION &&
                    it.treeParent.treeParent.elementType != VALUE_PARAMETER &&
                    it.treeParent.treeParent.elementType != VALUE_ARGUMENT &&
                    it.isPrecededByOtherAnnotationEntryOnTheSameLine() &&
                    it.isLastAnnotationEntry()
                // Code below is disallowed
                //   @Foo1 @Foo2 fun foo() {}
                // But following is allowed:
                //   @[Foo1 Foo2] fun foo() {}
                //   fun foo(@Bar1 @Bar2 bar) {}
            }
    }

    private fun ASTNode.hasAnnotationBeforeConstructor() =
        codeStyle == CodeStyleValue.ktlint_official &&
            hasAnnotationEntry() &&
            nextCodeSibling()?.elementType == CONSTRUCTOR_KEYWORD

    private fun ASTNode.hasAnnotationEntry() = children().any { it.elementType == ANNOTATION_ENTRY }

    private fun visitTypeArgumentList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .children()
            .filter { it.elementType == TYPE_PROJECTION }
            .mapNotNull { it.findChildByType(TYPE_REFERENCE) }
            .filter { it.elementType == TYPE_REFERENCE }
            .mapNotNull { it.findChildByType(MODIFIER_LIST) }
            .filter { it.elementType == MODIFIER_LIST }
            .any { it.shouldWrapAnnotations() }
            .ifTrue { wrapTypeArgumentList(node, emit, autoCorrect) }
    }

    private fun wrapTypeArgumentList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .children()
            .filter { it.elementType == TYPE_PROJECTION }
            .forEach { typeProjection ->
                val prevLeaf = typeProjection.prevLeaf().takeIf { it.isWhiteSpace() }
                if (prevLeaf == null || prevLeaf.isWhiteSpaceWithoutNewline()) {
                    emit(typeProjection.startOffset - 1, "Expected newline", true)
                    if (autoCorrect) {
                        typeProjection.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                    }
                }
            }

        node
            .findChildByType(GT)
            ?.let { gt ->
                val prevLeaf = gt.prevLeaf().takeIf { it.isWhiteSpace() }
                if (prevLeaf == null || prevLeaf.isWhiteSpaceWithoutNewline()) {
                    emit(gt.startOffset, "Expected newline", true)
                    if (autoCorrect) {
                        gt.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                    }
                }
            }
    }

    private fun visitAnnotationEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == ANNOTATION_ENTRY)
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

    private fun ASTNode.isPrecededByAnnotationOnAnotherLine(): Boolean {
        val firstAnnotation = treeParent.findChildByType(ANNOTATION_ENTRY)
        return siblings(forward = false)
            .takeWhile { it != firstAnnotation }
            .any { it.isWhiteSpaceWithNewline() }
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

    private fun ASTNode.isLastAnnotationEntry() =
        this ==
            treeParent
                .children()
                .lastOrNull { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.isPrecededByOtherAnnotationEntryWithoutParametersOnTheSameLine() =
        siblings(forward = false)
            .takeWhile { !it.isWhiteSpaceWithNewline() && !it.isAnnotationEntryWithValueArgumentList() }
            .any { it.elementType == ANNOTATION_ENTRY && !it.isAnnotationEntryWithValueArgumentList() }

    private fun ASTNode.isPrecededByOtherAnnotationEntryOnTheSameLine() =
        siblings(forward = false)
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

    private fun ASTNode?.annotationOnSameLineAsClosingParenthesisOfClassParameterList() =
        // Allow:
        //     class Foo(
        //         bar: Bar,
        //     ) : @Suppress("DEPRECATION")
        //         FooBar()
        this
            ?.takeIf { it.treeParent?.elementType == CLASS }
            ?.prevCodeSibling()
            ?.takeIf { it.elementType == COLON }
            ?.prevCodeLeaf()
            ?.takeIf { it.elementType == RPAR }
            ?.prevLeaf()
            ?.isWhiteSpaceWithNewline()
            ?: false

    private fun ASTNode.isOnSameLineAsNextAnnotationEntry() =
        siblings(forward = true)
            .takeWhile { it.elementType != ANNOTATION_ENTRY }
            .none { it.isWhiteSpaceWithNewline() }

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
        val nodeBeforeAnnotations =
            modifierListRoot
                .treeParent
                .treePrev
        // Make sure we only insert a single newline
        val indentWithoutNewline =
            nodeBeforeAnnotations
                ?.text
                .orEmpty()
                .substringAfterLast('\n')
        return "\n".plus(indentWithoutNewline)
    }

    private companion object {
        val ANNOTATION_CONTAINER =
            listOf(
                ANNOTATED_EXPRESSION,
                FILE_ANNOTATION_LIST,
                MODIFIER_LIST,
            )
    }
}

public val ANNOTATION_RULE_ID: RuleId = AnnotationRule().ruleId
