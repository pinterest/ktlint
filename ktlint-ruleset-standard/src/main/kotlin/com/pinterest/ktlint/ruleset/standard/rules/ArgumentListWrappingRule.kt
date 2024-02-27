package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lineLength
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting
 *
 * The rule is more aggressive in inserting newlines after arguments than mentioned in the styleguide:
 * Each argument should be on a separate line if
 * - at least one of the arguments is
 * - maxLineLength exceeded (and separating arguments with \n would actually help)
 * in addition, "(" and ")" must be on separates line if any of the arguments are (otherwise on the same)
 */
@SinceKtlint("0.1", STABLE)
public class ArgumentListWrappingRule :
    StandardRule(
        id = "argument-list-wrapping",
        visitorModifiers =
            setOf(
                // Disallow comments at unexpected locations in the value parameter list
                //     fun foo(
                //        bar /* some comment */: Bar
                //     )
                // or
                //     class Foo(
                //        bar /* some comment */: Bar
                //     )
                RunAfterRule(VALUE_ARGUMENT_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                RunAfterRule(WRAPPING_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                RunAfterRule(CLASS_SIGNATURE_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
            ),
        usesEditorConfigProperties =
            setOf(
                IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var editorConfigIndent = IndentConfig.DEFAULT_INDENT_CONFIG

    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue
    private var ignoreWhenParameterCountGreaterOrEqualThanProperty = UNSET_IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        editorConfigIndent =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        ignoreWhenParameterCountGreaterOrEqualThanProperty = editorConfig[IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (editorConfigIndent.disabled) {
            return
        }

        if (node.elementType == VALUE_ARGUMENT_LIST) {
            if (needToWrapArgumentList(node)) {
                node
                    .children()
                    .forEach { child -> wrapArgumentInList(child, emit, autoCorrect) }
            }
        }
    }

    private fun needToWrapArgumentList(node: ASTNode) =
        if ( // skip when there are no arguments
            node.firstChildNode?.treeNext?.elementType != RPAR &&
            // skip lambda arguments
            node.treeParent?.elementType != FUNCTION_LITERAL &&
            // skip if number of arguments is big
            node.children().count { it.elementType == VALUE_ARGUMENT } <= ignoreWhenParameterCountGreaterOrEqualThanProperty
        ) {
            // each argument should be on a separate line if
            // - at least one of the arguments is
            // - maxLineLength exceeded (and separating arguments with \n would actually help)
            // in addition, "(" and ")" must be on separates line if any of the arguments are (otherwise on the same)
            node.textContainsIgnoringLambda('\n') || node.exceedsMaxLineLength()
        } else {
            false
        }

    private fun ASTNode.exceedsMaxLineLength() = lineLength(excludeEolComment = true) > maxLineLength && !textContains('\n')

    private fun intendedIndent(child: ASTNode): String =
        when {
            // IDEA quirk:
            // generic<
            //     T,
            //     R>(
            //     1,
            //     2
            // )
            // instead of
            // generic<
            //     T,
            //     R>(
            //         1,
            //         2
            //     )
            child.treeParent.hasTypeArgumentListInFront() -> -1
            // IDEA quirk:
            // foo
            //     .bar = Baz(
            //     1,
            //     2
            // )
            // instead of
            // foo
            //     .bar = Baz(
            //         1,
            //         2
            //     )
            child.treeParent.isPartOfDotQualifiedAssignmentExpression() -> -1
            else -> 0
        }.let {
            if (child.treeParent.isOnSameLineAsControlFlowKeyword()) {
                it + 1
            } else {
                it
            }
        }.let {
            if (child.elementType == VALUE_ARGUMENT) {
                it + 1
            } else {
                it
            }
        }.let { indentLevelFix ->
            val indentLevel =
                editorConfigIndent
                    .indentLevelFrom(child.treeParent.indent(false))
                    .plus(indentLevelFix)
            "\n" + editorConfigIndent.indent.repeat(maxOf(indentLevel, 0))
        }

    private fun wrapArgumentInList(
        child: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        when (child.elementType) {
            LPAR -> {
                val prevLeaf = child.prevLeaf()
                if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) {
                    emit(child.startOffset, errorMessage(child), true)
                    if (autoCorrect) {
                        prevLeaf.delete()
                    }
                }
            }

            VALUE_ARGUMENT,
            RPAR,
            -> {
                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_PARAMETER...
                // <line indent> RPAR
                val intendedIndent = intendedIndent(child)
                val prevLeaf = child.prevWhiteSpaceWithNewLine() ?: child.prevLeaf()
                if (prevLeaf is PsiWhiteSpace) {
                    if (prevLeaf.getText().contains("\n")) {
                        // The current child is already wrapped to a new line. Checking and fixing the
                        // correct size of the indent is the responsibility of the IndentationRule.
                        return
                    } else {
                        // The current child needs to be wrapped to a newline.
                        emit(child.startOffset, errorMessage(child), true)
                        if (autoCorrect) {
                            // The indentation is purely based on the previous leaf only. Note that in
                            // autoCorrect mode the indent rule, if enabled, runs after this rule and
                            // determines the final indentation. But if the indent rule is disabled then the
                            // indent of this rule is kept.
                            (prevLeaf as LeafPsiElement).rawReplaceWithText(intendedIndent)
                        }
                    }
                } else {
                    // Insert a new whitespace element in order to wrap the current child to a new line.
                    emit(child.startOffset, errorMessage(child), true)
                    if (autoCorrect) {
                        child.treeParent.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                    }
                }
                // Indentation of child nodes need to be fixed by the IndentationRule.
            }
        }
    }

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            LPAR -> """Unnecessary newline before "(""""
            VALUE_ARGUMENT -> "Argument should be on a separate line (unless all arguments can fit a single line)"
            RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }

    private fun ASTNode.textContainsIgnoringLambda(char: Char): Boolean =
        children().any { child ->
            child.isWhitespaceContaining(char) ||
                child.isCollectionLiteralContaining(char) ||
                child.isValueArgumentContaining(char)
        }

    private fun ASTNode.isWhitespaceContaining(char: Char) = elementType == WHITE_SPACE && textContains(char)

    private fun ASTNode.isCollectionLiteralContaining(char: Char) = elementType == COLLECTION_LITERAL_EXPRESSION && textContains(char)

    private fun ASTNode.isValueArgumentContaining(char: Char) =
        elementType == VALUE_ARGUMENT && children().any { it.textContainsIgnoringLambda(char) }

    private fun ASTNode.hasTypeArgumentListInFront(): Boolean =
        treeParent
            .children()
            .firstOrNull { it.elementType == TYPE_ARGUMENT_LIST }
            ?.children()
            ?.any { it.isWhiteSpaceWithNewline() } == true

    private fun ASTNode.isPartOfDotQualifiedAssignmentExpression(): Boolean =
        treeParent
            ?.treeParent
            ?.takeIf { it.elementType == BINARY_EXPRESSION }
            ?.let { binaryExpression ->
                binaryExpression.firstChildNode.elementType == DOT_QUALIFIED_EXPRESSION &&
                    binaryExpression.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType == EQ
            }
            ?: false

    private fun ASTNode.prevWhiteSpaceWithNewLine(): ASTNode? {
        var prev = prevLeaf()
        while (prev != null && (prev.isWhiteSpace() || prev.isPartOfComment())) {
            if (prev.isWhiteSpaceWithNewline()) {
                return prev
            }
            prev = prev.prevLeaf()
        }
        return null
    }

    private fun ASTNode.isOnSameLineAsControlFlowKeyword(): Boolean {
        val containerNode = psi.getStrictParentOfType<KtContainerNode>() ?: return false
        if (containerNode.node.elementType == ELSE) return false
        val controlFlowKeyword =
            when (val parent = containerNode.parent) {
                is KtIfExpression -> parent.ifKeyword.node
                is KtWhileExpression -> parent.firstChild.node
                is KtDoWhileExpression -> parent.whileKeyword?.node
                else -> null
            } ?: return false

        var prevLeaf = prevLeaf() ?: return false
        while (prevLeaf != controlFlowKeyword) {
            if (prevLeaf.isWhiteSpaceWithNewline()) return false
            prevLeaf = prevLeaf.prevLeaf() ?: return false
        }
        return true
    }

    public companion object {
        private const val UNSET_IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY = Int.MAX_VALUE
        public val IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY: EditorConfigProperty<Int> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_argument_list_wrapping_ignore_when_parameter_count_greater_or_equal_than",
                        "Do not wrap parameters on separate lines when at least the specified number of parameters are " +
                            "specified. Use 'unset' to always wrap each parameter.",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset"),
                    ),
                // Historically, all code styles have used 8 as the magic value.
                defaultValue = 8,
                ktlintOfficialCodeStyleDefaultValue = UNSET_IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY,
                propertyMapper = { property, _ ->
                    if (property?.isUnset == true) {
                        UNSET_IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY
                    } else {
                        property?.getValueAs<Int>()
                    }
                },
                propertyWriter = { property ->
                    if (property == UNSET_IGNORE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY) {
                        "unset"
                    } else {
                        property.toString()
                    }
                },
            )
    }
}

public val ARGUMENT_LIST_WRAPPING_RULE_ID: RuleId = ArgumentListWrappingRule().ruleId
