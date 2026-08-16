package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ARROW
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS_BODY
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.COMMA
import io.github.ktlint.core.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.ENUM_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.ENUM_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUNCTION_TYPE
import io.github.ktlint.core.rule.engine.core.api.ElementType.GT
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.LPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.SEMICOLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHEN_ENTRY
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfigProperty
import io.github.ktlint.core.rule.engine.core.api.hasModifier
import io.github.ktlint.core.rule.engine.core.api.hasNewLineInClosedRange
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indent
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.noNewLineInClosedRange
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevCodeLeaf
import io.github.ktlint.core.rule.engine.core.api.prevCodeSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.util.cast
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.KtNodeTypes.WHEN_ENTRY_GUARD
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = node.closingElementDestructuringDeclarationEntries(),
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
                .children
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
                .children
                .lastOrNull { it.elementType == RPAR }
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
                .children
                .first { it.elementType == GT }
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
                .children
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
            .takeIf { node.hasModifier(ENUM_KEYWORD) }
            ?.findChildByType(CLASS_BODY)
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

    private fun ASTNode.noEnumEntries() = children.none { it.elementType == ENUM_ENTRY }

    private fun ASTNode.lastTwoEnumEntriesAreOnSameLine(): Boolean {
        val lastTwoEnumEntries =
            this@lastTwoEnumEntriesAreOnSameLine
                .children
                .filter { it.elementType == ENUM_ENTRY }
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
        children
            .lastOrNull { it.elementType == ENUM_ENTRY }
            ?.children
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
                            if (!lastNodeBeforeArrow.isWhiteSpaceWithNewline) {
                                emit(
                                    trailingCommaNode!!.startOffset,
                                    "Expected a newline between the trailing comma and  \"${inspectNode.text}\"",
                                    true,
                                ).ifAutocorrectAllowed {
                                    lastNodeBeforeArrow.upsertWhitespaceAfterMe(inspectNode.parent!!.indent)
                                }
                            }
                        }
                } else {
                    emit(
                        trailingCommaNode!!.startOffset,
                        "Unnecessary trailing comma before \"${inspectNode.text}\"",
                        true,
                    ).ifAutocorrectAllowed { trailingCommaNode.remove() }
                }
            }

            TrailingCommaState.MISSING -> {
                if (isTrailingCommaAllowed) {
                    val leafBeforeArrowOrNull = leafBeforeArrowOrNull()
                    val addNewLine = !(leafBeforeArrowOrNull?.isWhiteSpaceWithNewline ?: true)
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
                            val indent = prevNode.parent!!.indent
                            if (leafBeforeArrowOrNull.isWhiteSpace) {
                                leafBeforeArrowOrNull.replaceTextWith(indent)
                            } else {
                                inspectNode
                                    .prevCodeLeaf
                                    ?.nextLeaf
                                    ?.let { before ->
                                        before.parent?.addChild(PsiWhiteSpaceImpl(indent), before)
                                    }
                            }
                        }

                        if (inspectNode.parent?.elementType == ENUM_ENTRY) {
                            val parentIndent =
                                (prevNode.parent?.prevLeaf?.takeIf { it.isWhiteSpace })?.text
                                    ?: prevNode.indent
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
                ).ifAutocorrectAllowed { trailingCommaNode.remove() }
            }

            TrailingCommaState.NOT_EXISTS -> {
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
                hasNewLineInClosedRange(
                    // Get the LPAR or LBRACKET before the first entry
                    openingElementDestructuringDeclarationEntries(),
                    // Get the RPAR or RBRACKET after the last entry
                    closingElementDestructuringDeclarationEntries(),
                )
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

    private fun ASTNode.openingElementDestructuringDeclarationEntries(): ASTNode {
        require(elementType == DESTRUCTURING_DECLARATION)
        return findChildByType(DESTRUCTURING_DECLARATION_ENTRY)!!
            .prevCodeSibling!!
            .also { require(it.elementType == LPAR || it.elementType == LBRACKET) }
    }

    private fun ASTNode.closingElementDestructuringDeclarationEntries(): ASTNode {
        require(elementType == DESTRUCTURING_DECLARATION)
        return children()
            .last { it.elementType == DESTRUCTURING_DECLARATION_ENTRY }
            .nextSibling { it.isCode && it.elementType != COMMA }!!
            .also { require(it.elementType == RPAR || it.elementType == RBRACKET) }
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

    private fun ASTNode.hasWhenEntryGuardKotlin21(): Boolean = children.any { it.elementType == WHEN_ENTRY_GUARD }

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
