package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INDICES
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Linting trailing comma for call site.
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html#trailing-commas)
 */
@SinceKtlint("0.43", EXPERIMENTAL)
@SinceKtlint("0.47", STABLE)
public class TrailingCommaOnCallSiteRule :
    StandardRule(
        id = "trailing-comma-on-call-site",
        visitorModifiers =
            setOf(
                VisitorModifier.RunAfterRule(
                    ruleId = WRAPPING_RULE_ID,
                    mode = ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                ),
                VisitorModifier.RunAsLateAsPossible,
            ),
        usesEditorConfigProperties = setOf(TRAILING_COMMA_ON_CALL_SITE_PROPERTY),
    ) {
    private var allowTrailingCommaOnCallSite = TRAILING_COMMA_ON_CALL_SITE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        allowTrailingCommaOnCallSite = editorConfig[TRAILING_COMMA_ON_CALL_SITE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            COLLECTION_LITERAL_EXPRESSION -> visitCollectionLiteralExpression(node, emit)
            INDICES -> visitIndices(node, emit)
            TYPE_ARGUMENT_LIST -> visitTypeList(node, emit)
            VALUE_ARGUMENT_LIST -> visitValueList(node, emit)
            else -> Unit
        }
    }

    private fun visitCollectionLiteralExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val inspectNode =
            node
                .children()
                .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
        )
    }

    private fun ASTNode.isTrailingCommaAllowed() = elementType in TYPES_ON_CALL_SITE && allowTrailingCommaOnCallSite

    private fun visitIndices(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val inspectNode =
            node
                .children()
                .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
        )
    }

    private fun visitValueList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.treeParent.elementType != ElementType.FUNCTION_LITERAL) {
            node
                .children()
                .lastOrNull { it.elementType == ElementType.RPAR }
                ?.let { inspectNode ->
                    node.reportAndCorrectTrailingCommaNodeBefore(
                        inspectNode = inspectNode,
                        emit = emit,
                        isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                    )
                }
        }
    }

    private fun visitTypeList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val inspectNode =
            node
                .children()
                .first { it.elementType == ElementType.GT }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
        )
    }

    private fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode,
        isTrailingCommaAllowed: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prevLeaf = inspectNode.prevLeaf()
        val trailingCommaNode = prevLeaf?.findPreviousTrailingCommaNodeOrNull()
        val trailingCommaState =
            when {
                this.isMultiline() -> if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
                else -> if (trailingCommaNode != null) TrailingCommaState.REDUNDANT else TrailingCommaState.NOT_EXISTS
            }
        when (trailingCommaState) {
            TrailingCommaState.EXISTS -> {
                if (!isTrailingCommaAllowed) {
                    emit(
                        trailingCommaNode!!.startOffset,
                        "Unnecessary trailing comma before \"${inspectNode.text}\"",
                        true,
                    ).ifAutocorrectAllowed {
                        this.removeChild(trailingCommaNode)
                    }
                }
            }

            TrailingCommaState.MISSING -> {
                if (isTrailingCommaAllowed) {
                    val prevNode = inspectNode.prevCodeLeaf()!!
                    emit(
                        prevNode.startOffset + prevNode.textLength,
                        "Missing trailing comma before \"${inspectNode.text}\"",
                        true,
                    ).ifAutocorrectAllowed {
                        inspectNode
                            .prevCodeSibling()
                            ?.nextSibling()
                            ?.let { before ->
                                before.treeParent.addChild(LeafPsiElement(COMMA, ","), before)
                            }
                    }
                }
            }

            TrailingCommaState.REDUNDANT -> {
                emit(
                    trailingCommaNode!!.startOffset,
                    "Unnecessary trailing comma before \"${inspectNode.text}\"",
                    true,
                ).ifAutocorrectAllowed {
                    this.removeChild(trailingCommaNode)
                }
            }

            TrailingCommaState.NOT_EXISTS -> {
                Unit
            }
        }
    }

    private fun ASTNode.isMultiline(): Boolean =
        if (elementType == VALUE_ARGUMENT_LIST) {
            hasAtLeastOneArgument() && hasValueArgumentFollowedByWhiteSpaceWithNewline()
        } else {
            textContains('\n')
        }

    private fun ASTNode.hasValueArgumentFollowedByWhiteSpaceWithNewline(): Boolean =
        findValueArgumentFollowedByWhiteSpaceWithNewline() != null

    private fun ASTNode.findValueArgumentFollowedByWhiteSpaceWithNewline() =
        this
            .findChildByType(VALUE_ARGUMENT)
            ?.nextSibling { it.isWhiteSpaceWithNewline20 }

    private fun ASTNode.hasAtLeastOneArgument() = children().any { it.elementType == VALUE_ARGUMENT }

    private fun ASTNode.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        val codeLeaf =
            if (isCodeLeaf()) {
                this
            } else {
                prevCodeLeaf()
            }
        return codeLeaf?.takeIf { it.elementType == COMMA }
    }

    private enum class TrailingCommaState {
        /**
         * The trailing comma is needed and exists
         */
        EXISTS,

        /**
         * The trailing comma is needed and doesn't exist
         */
        MISSING,

        /**
         * The trailing comma isn't needed and doesn't exist
         */
        NOT_EXISTS,

        /**
         * The trailing comma isn't needed, but exists
         */
        REDUNDANT,
    }

    public companion object {
        private val BOOLEAN_VALUES_SET = setOf("true", "false")

        public val TRAILING_COMMA_ON_CALL_SITE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_kotlin_allow_trailing_comma_on_call_site",
                        "Defines whether a trailing comma (or no trailing comma) should be enforced on the calling side," +
                            "e.g. argument-list, when-entries, lambda-arguments, indices, etc." +
                            "When set, IntelliJ IDEA uses this property to allow usage of a trailing comma by discretion " +
                            "of the developer. KtLint however uses this setting to enforce consistent usage of the " +
                            "trailing comma when set.",
                        PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        BOOLEAN_VALUES_SET,
                    ),
                defaultValue = true,
                androidStudioCodeStyleDefaultValue = false,
            )

        private val TYPES_ON_CALL_SITE =
            TokenSet.create(
                COLLECTION_LITERAL_EXPRESSION,
                INDICES,
                TYPE_ARGUMENT_LIST,
                VALUE_ARGUMENT_LIST,
            )
    }
}

public val TRAILING_COMMA_ON_CALL_SITE_RULE_ID: RuleId = TrailingCommaOnCallSiteRule().ruleId
