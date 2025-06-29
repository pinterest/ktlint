package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.util.cast
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.KtNodeTypes.WHEN_ENTRY_GUARD
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.children

/**
 * Linting trailing comma for declaration site.
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html#trailing-commas)
 */
@SinceKtlint("0.43", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class TrailingCommaOnDeclarationSiteRule :
    StandardRule(
        id = "trailing-comma-on-declaration-site",
        visitorModifiers =
            setOf(
                VisitorModifier.RunAfterRule(
                    ruleId = WRAPPING_RULE_ID,
                    mode = ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                ),
                VisitorModifier.RunAsLateAsPossible,
            ),
        usesEditorConfigProperties = setOf(TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY),
    ) {
    private var allowTrailingComma = TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        allowTrailingComma = editorConfig[TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            CLASS -> visitClass(node, emit)
            DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node, emit)
            FUNCTION_LITERAL -> visitFunctionLiteral(node, emit)
            TYPE_PARAMETER_LIST -> visitTypeList(node, emit)
            VALUE_PARAMETER_LIST -> visitValueList(node, emit)
            WHEN_ENTRY -> visitWhenEntry(node, emit)
            else -> Unit
        }
    }

    private fun visitDestructuringDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val inspectNode =
            node
                .children20
                .last { it.elementType == ElementType.RPAR }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            emit = emit,
        )
    }

    private fun ASTNode.isTrailingCommaAllowed() = elementType in TYPES_ON_DECLARATION_SITE && allowTrailingComma

    private fun visitFunctionLiteral(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val inspectNode =
            node
                .children20
                .lastOrNull { it.elementType == ARROW }
                ?: // lambda w/o an arrow -> no arguments -> no commas
                return
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            emit = emit,
        )
    }

    private fun visitValueList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.parent?.elementType != FUNCTION_LITERAL) {
            node
                .children20
                .lastOrNull { it.elementType == ElementType.RPAR }
                ?.let { inspectNode ->
                    node.reportAndCorrectTrailingCommaNodeBefore(
                        inspectNode = inspectNode,
                        isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                        emit = emit,
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
                .children20
                .first { it.elementType == ElementType.GT }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            emit = emit,
        )
    }

    private fun visitWhenEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val psi = node.psi
        require(psi is KtWhenEntry)
        if (psi.isElse || psi.parent.cast<KtWhenExpression>().leftParenthesis == null) {
            // no commas for "else" or when there are no opening parenthesis for the when-expression
            return
        }

        val inspectNode =
            node
                .children20
                .first { it.elementType == ARROW }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            emit = emit,
        )
    }

    private fun visitClass(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == CLASS)

        node
            .takeIf { node.hasModifier(ElementType.ENUM_KEYWORD) }
            ?.findChildByType(ElementType.CLASS_BODY)
            ?.takeUnless { it.noEnumEntries() }
            ?.let { classBody ->
                classBody
                    .findNodeAfterLastEnumEntry()
                    ?.let { nodeAfterLastEnumEntry ->
                        when {
                            !node.isTrailingCommaAllowed() && nodeAfterLastEnumEntry.elementType == RBRACE -> {
                                node.reportAndCorrectTrailingCommaNodeBefore(
                                    inspectNode = nodeAfterLastEnumEntry,
                                    isTrailingCommaAllowed = false,
                                    emit = emit,
                                )
                            }

                            !classBody.lastTwoEnumEntriesAreOnSameLine() -> {
                                node.reportAndCorrectTrailingCommaNodeBefore(
                                    inspectNode = nodeAfterLastEnumEntry,
                                    isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                                    emit = emit,
                                )
                            }
                        }
                    }
            }
    }

    private fun ASTNode.noEnumEntries() = children20.none { it.elementType == ElementType.ENUM_ENTRY }

    private fun ASTNode.lastTwoEnumEntriesAreOnSameLine(): Boolean {
        val lastTwoEnumEntries =
            children20
                .filter { it.elementType == ElementType.ENUM_ENTRY }
                .toList()
                .takeLast(2)

        return lastTwoEnumEntries.count() == 2 && noNewLineInClosedRange(lastTwoEnumEntries[0], lastTwoEnumEntries[1])
    }

    /**
     * Determines the [ASTNode] before which the trailing comma is allowed.
     *
     * If the list of enumeration entries is terminated by a semicolon, that semicolon will be returned. Otherwise, the
     * last element of the class.
     */
    private fun ASTNode.findNodeAfterLastEnumEntry() =
        children20
            .lastOrNull { it.elementType == ElementType.ENUM_ENTRY }
            ?.children20
            ?.singleOrNull { it.elementType == SEMICOLON }
            ?: lastChildNode

    private fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode,
        isTrailingCommaAllowed: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prevLeaf = inspectNode.prevLeaf
        val trailingCommaNode = prevLeaf?.findPreviousTrailingCommaNodeOrNull()
        val trailingCommaState =
            when {
                hasWhenEntryGuard() -> {
                    // The compiler won't allow any comma in the when-entry in case it contains a guard clause
                    TrailingCommaState.NOT_EXISTS
                }

                isMultiline() -> {
                    if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
                }

                else -> {
                    if (trailingCommaNode != null) TrailingCommaState.REDUNDANT else TrailingCommaState.NOT_EXISTS
                }
            }
        when (trailingCommaState) {
            TrailingCommaState.EXISTS -> {
                if (isTrailingCommaAllowed) {
                    inspectNode
                        .parent
                        ?.takeIf { it.elementType == WHEN_ENTRY }
                        ?.findChildByType(ARROW)
                        ?.prevLeaf
                        ?.let { lastNodeBeforeArrow ->
                            if (!lastNodeBeforeArrow.isWhiteSpaceWithNewline20) {
                                emit(
                                    trailingCommaNode!!.startOffset,
                                    "Expected a newline between the trailing comma and  \"${inspectNode.text}\"",
                                    true,
                                ).ifAutocorrectAllowed {
                                    lastNodeBeforeArrow.upsertWhitespaceAfterMe(inspectNode.parent!!.indent20)
                                }
                            }
                        }
                } else {
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
                    val leafBeforeArrowOrNull = leafBeforeArrowOrNull()
                    val addNewLine = !(leafBeforeArrowOrNull?.isWhiteSpaceWithNewline20 ?: true)
                    val prevNode = inspectNode.prevCodeLeaf!!
                    if (addNewLine) {
                        emit(
                            prevNode.startOffset + prevNode.textLength,
                            "Missing trailing comma and newline before \"${inspectNode.text}\"",
                            true,
                        )
                    } else {
                        emit(
                            prevNode.startOffset + prevNode.textLength,
                            "Missing trailing comma before \"${inspectNode.text}\"",
                            true,
                        )
                    }.ifAutocorrectAllowed {
                        if (addNewLine) {
                            val indent = prevNode.parent!!.indent20
                            if (leafBeforeArrowOrNull.isWhiteSpace20) {
                                (leafBeforeArrowOrNull as LeafElement).rawReplaceWithText(indent)
                            } else {
                                inspectNode
                                    .prevCodeLeaf
                                    ?.nextLeaf
                                    ?.let { before ->
                                        before.parent?.addChild(PsiWhiteSpaceImpl(indent), before)
                                    }
                            }
                        }

                        if (inspectNode.parent?.elementType == ElementType.ENUM_ENTRY) {
                            val parentIndent =
                                (prevNode.parent?.prevLeaf?.takeIf { it.isWhiteSpace20 })?.text
                                    ?: prevNode.indent20
                            inspectNode
                                .parent
                                ?.apply {
                                    addChild(LeafPsiElement(COMMA, ","), inspectNode)
                                    addChild(PsiWhiteSpaceImpl(parentIndent), null)
                                    addChild(LeafPsiElement(SEMICOLON, ";"), null)
                                }
                            inspectNode.remove()
                        } else {
                            inspectNode
                                .prevCodeLeaf
                                ?.nextLeaf
                                ?.let { before ->
                                    before.parent?.addChild(LeafPsiElement(COMMA, ","), before)
                                }
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
        when {
            parent?.elementType == FUNCTION_LITERAL -> {
                parent!!.isMultiline()
            }

            elementType == FUNCTION_LITERAL -> {
                hasNewLineInClosedRange(findChildByType(VALUE_PARAMETER_LIST)!!, findChildByType(ARROW)!!)
            }

            elementType == WHEN_ENTRY -> {
                hasNewLineInClosedRange(firstChildNode, findChildByType(ARROW)!!)
            }

            elementType == DESTRUCTURING_DECLARATION -> {
                hasNewLineInClosedRange(findChildByType(LPAR)!!, findChildByType(RPAR)!!)
            }

            elementType == VALUE_ARGUMENT_LIST &&
                children().count { it.elementType == VALUE_ARGUMENT } == 1 &&
                findChildByType(VALUE_ARGUMENT_LIST)!!.elementType == COLLECTION_LITERAL_EXPRESSION -> {
                // special handling for collection literal
                // @Annotation([
                //    "something",
                // ])
                hasNewLineInClosedRange(findChildByType(RBRACKET)!!, findChildByType(RPAR)!!)
            }

            elementType == VALUE_PARAMETER_LIST && findChildByType(VALUE_PARAMETER) == null -> {
                false
            }

            else -> {
                textContains('\n')
            }
        }

    private fun ASTNode.leafBeforeArrowOrNull() =
        takeIf { it.elementType == WHEN_ENTRY || it.elementType == FUNCTION_LITERAL }
            ?.findChildByType(ARROW)
            ?.prevLeaf

    private fun ASTNode.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        val codeLeaf =
            if (isCode) {
                this
            } else {
                prevCodeLeaf
            }
        return codeLeaf?.takeIf { it.elementType == COMMA }
    }

    private fun ASTNode.hasWhenEntryGuard() = elementType == WHEN_ENTRY && hasWhenEntryGuardKotlin21()

    private fun ASTNode.hasWhenEntryGuardKotlin21(): Boolean = children20.any { it.elementType == WHEN_ENTRY_GUARD }

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

        public val TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_kotlin_allow_trailing_comma",
                        "Defines whether a trailing comma (or no trailing comma) should be enforced on the defining " +
                            "side, e.g. parameter-list, type-argument-list, lambda-value-parameters, enum-entries, etc." +
                            "When set, IntelliJ IDEA uses this property to allow usage of a trailing comma by discretion " +
                            "of the developer. KtLint however uses this setting to enforce consistent usage of the " +
                            "trailing comma when set.",
                        PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        BOOLEAN_VALUES_SET,
                    ),
                defaultValue = true,
                androidStudioCodeStyleDefaultValue = false,
            )

        private val TYPES_ON_DECLARATION_SITE =
            TokenSet.create(
                CLASS,
                DESTRUCTURING_DECLARATION,
                FUNCTION_LITERAL,
                FUNCTION_TYPE,
                TYPE_PARAMETER_LIST,
                VALUE_PARAMETER_LIST,
                WHEN_ENTRY,
            )
    }
}

public val TRAILING_COMMA_ON_DECLARATION_SITE_RULE_ID: RuleId = TrailingCommaOnDeclarationSiteRule().ruleId
