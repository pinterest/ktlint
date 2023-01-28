package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.DESTRUCTURING_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isCodeLeaf
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.noNewLineInClosedRange
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import kotlin.properties.Delegates
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.utils.addToStdlib.cast

/**
 * Linting trailing comma for declaration site.
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html#trailing-commas)
 */
public class TrailingCommaOnDeclarationSiteRule :
    Rule(
        id = "trailing-comma-on-declaration-site",
        visitorModifiers = setOf(
            VisitorModifier.RunAfterRule(
                ruleId = "standard:wrapping",
                loadOnlyWhenOtherRuleIsLoaded = true,
                runOnlyWhenOtherRuleIsEnabled = true,
            ),
            VisitorModifier.RunAsLateAsPossible,
        ),
    ),
    UsesEditorConfigProperties {

    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY,
    )

    private var allowTrailingComma by Delegates.notNull<Boolean>()

    private fun ASTNode.isTrailingCommaAllowed() =
        elementType in TYPES_ON_DECLARATION_SITE && allowTrailingComma

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        allowTrailingComma = editorConfigProperties.getEditorConfigValue(TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            CLASS -> visitClass(node, emit, autoCorrect)
            DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node, autoCorrect, emit)
            FUNCTION_LITERAL -> visitFunctionLiteral(node, autoCorrect, emit)
            TYPE_PARAMETER_LIST -> visitTypeList(node, autoCorrect, emit)
            VALUE_PARAMETER_LIST -> visitValueList(node, autoCorrect, emit)
            WHEN_ENTRY -> visitWhenEntry(node, autoCorrect, emit)
            else -> Unit
        }
    }

    private fun visitDestructuringDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val inspectNode = node
            .children()
            .last { it.elementType == ElementType.RPAR }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
            emit = emit,
        )
    }

    private fun visitFunctionLiteral(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val inspectNode = node
            .children()
            .lastOrNull { it.elementType == ARROW }
            ?: // lambda w/o an arrow -> no arguments -> no commas
            return
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
            emit = emit,
        )
    }

    private fun visitValueList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.treeParent.elementType != FUNCTION_LITERAL) {
            node
                .children()
                .lastOrNull { it.elementType == ElementType.RPAR }
                ?.let { inspectNode ->
                    node.reportAndCorrectTrailingCommaNodeBefore(
                        inspectNode = inspectNode,
                        isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                        autoCorrect = autoCorrect,
                        emit = emit,
                    )
                }
        }
    }

    private fun visitTypeList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val inspectNode = node
            .children()
            .first { it.elementType == ElementType.GT }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
            emit = emit,
        )
    }

    private fun visitWhenEntry(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val psi = node.psi
        require(psi is KtWhenEntry)
        if (psi.isElse || psi.parent.cast<KtWhenExpression>().leftParenthesis == null) {
            // no commas for "else" or when there are no opening parenthesis for the when-expression
            return
        }

        val inspectNode = node
            .children()
            .first { it.elementType == ARROW }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
            emit = emit,
        )
    }
    private fun visitClass(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val psi = node.psi
        require(psi is KtClass)

        node
            .takeIf { psi.isEnum() }
            ?.findChildByType(ElementType.CLASS_BODY)
            ?.takeUnless {
                // Do nothing when last two entries are on same line as no trailing comma should be inserted
                it.lastTwoEnumEntriesAreOnSameLine()
            }?.let { classBody ->
                val nodeAfterTrailingCommaPosition = classBody.findNodeAfterTrailingCommaPosition()
                node.reportAndCorrectTrailingCommaNodeBefore(
                    inspectNode = nodeAfterTrailingCommaPosition,
                    isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                    autoCorrect = autoCorrect,
                    emit = emit,
                )
            }
    }

    /**
     * Determines the [ASTNode] before which the trailing comma is allowed.
     *
     * If the list of enumeration entries is terminated by a semicolon, that semicolon will be returned. Otherwise, the
     * last element of the class.
     */
    private fun ASTNode.findNodeAfterTrailingCommaPosition(): ASTNode {
        val lastEnumEntry = children().last { it.psi is KtEnumEntry }

        val semicolonAfterLastEnumEntry = lastEnumEntry
            .children()
            .singleOrNull { it.elementType == ElementType.SEMICOLON }

        return semicolonAfterLastEnumEntry ?: lastChildNode
    }

    private fun ASTNode.lastTwoEnumEntriesAreOnSameLine(): Boolean {
        val lastTwoEnumEntries =
            children()
                .filter { it.psi is KtEnumEntry }
                .toList()
                .takeLast(2)

        return lastTwoEnumEntries.count() == 2 && noNewLineInClosedRange(lastTwoEnumEntries[0], lastTwoEnumEntries[1])
    }

    private fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode,
        isTrailingCommaAllowed: Boolean,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val prevLeaf = inspectNode.prevLeaf()
        val trailingCommaNode = prevLeaf?.findPreviousTrailingCommaNodeOrNull()
        val trailingCommaState = when {
            isMultiline(psi) -> if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
            else -> if (trailingCommaNode != null) TrailingCommaState.REDUNDANT else TrailingCommaState.NOT_EXISTS
        }
        when (trailingCommaState) {
            TrailingCommaState.EXISTS ->
                if (isTrailingCommaAllowed) {
                    inspectNode
                        .treeParent
                        .takeIf { it.elementType == WHEN_ENTRY }
                        ?.findChildByType(ARROW)
                        ?.prevLeaf()
                        ?.let { lastNodeBeforeArrow ->
                            if (lastNodeBeforeArrow.elementType != WHITE_SPACE || !lastNodeBeforeArrow.textContains('\n')) {
                                emit(
                                    trailingCommaNode!!.startOffset,
                                    "Expected a newline between the trailing comma and  \"${inspectNode.text}\"",
                                    true,
                                )
                                if (autoCorrect) {
                                    val parentIndent = "\n" + inspectNode.getWhenEntryIndent()
                                    lastNodeBeforeArrow.upsertWhitespaceAfterMe(parentIndent)
                                }
                            }
                        }
                } else {
                    emit(
                        trailingCommaNode!!.startOffset,
                        "Unnecessary trailing comma before \"${inspectNode.text}\"",
                        true,
                    )
                    if (autoCorrect) {
                        this.removeChild(trailingCommaNode)
                    }
                }
            TrailingCommaState.MISSING ->
                if (isTrailingCommaAllowed) {
                    val addNewLineBeforeArrowInWhenEntry = addNewLineBeforeArrowInWhen()
                    val prevNode = inspectNode.prevCodeLeaf()!!
                    if (addNewLineBeforeArrowInWhenEntry) {
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
                    }
                    if (autoCorrect) {
                        if (addNewLineBeforeArrowInWhenEntry) {
                            val parentIndent = "\n" + prevNode.getWhenEntryIndent()
                            val leafBeforeArrow = (psi as KtWhenEntry).arrow?.prevLeaf()
                            if (leafBeforeArrow != null && leafBeforeArrow is PsiWhiteSpace) {
                                val newLine = KtPsiFactory(prevNode.psi).createWhiteSpace(parentIndent)
                                leafBeforeArrow.replace(newLine)
                            } else {
                                val newLine = KtPsiFactory(prevNode.psi).createWhiteSpace(parentIndent)
                                prevNode.psi.parent.addAfter(newLine, prevNode.psi)
                            }
                        }

                        val comma = KtPsiFactory(prevNode.psi).createComma()
                        if (inspectNode.treeParent.elementType == ElementType.ENUM_ENTRY) {
                            with(KtPsiFactory(prevNode.psi)) {
                                val parentIndent =
                                    (prevNode.psi.parent.prevLeaf() as? PsiWhiteSpace)?.text
                                        ?: "\n${prevNode.lineIndent()}"
                                val newline = createWhiteSpace(parentIndent)
                                val enumEntry = inspectNode.treeParent.psi
                                enumEntry.apply {
                                    inspectNode.psi.replace(comma)
                                    add(newline)
                                    add(createSemicolon())
                                }
                            }
                            Unit
                        } else {
                            prevNode.psi.parent.addAfter(comma, prevNode.psi)
                        }
                    }
                }
            TrailingCommaState.REDUNDANT -> {
                emit(
                    trailingCommaNode!!.startOffset,
                    "Unnecessary trailing comma before \"${inspectNode.text}\"",
                    true,
                )
                if (autoCorrect) {
                    this.removeChild(trailingCommaNode)
                }
            }
            TrailingCommaState.NOT_EXISTS -> Unit
        }
    }

    private fun ASTNode.getWhenEntryIndent() =
        // The when entry can be a simple value but might also be a complex expression.
        parents()
            .last { it.elementType == WHEN_ENTRY }
            .lineIndent()

    private fun isMultiline(element: PsiElement): Boolean = when {
        element.parent is KtFunctionLiteral -> isMultiline(element.parent)
        element is KtFunctionLiteral -> containsLineBreakInLeavesRange(element.valueParameterList!!, element.arrow!!)
        element is KtWhenEntry -> containsLineBreakInLeavesRange(element.firstChild, element.arrow!!)
        element is KtDestructuringDeclaration -> containsLineBreakInLeavesRange(element.lPar!!, element.rPar!!)
        element is KtValueArgumentList && element.children.size == 1 && element.anyDescendantOfType<KtCollectionLiteralExpression>() -> {
            // special handling for collection literal
            // @Annotation([
            //    "something",
            // ])
            val lastChild = element.collectDescendantsOfType<KtCollectionLiteralExpression>().last()
            containsLineBreakInLeavesRange(lastChild.rightBracket!!, element.rightParenthesis!!)
        }
        element is KtParameterList && element.parameters.isEmpty() -> false
        else -> element.textContains('\n')
    }

    private fun ASTNode.addNewLineBeforeArrowInWhen() =
        if (psi is KtWhenEntry) {
            val leafBeforeArrow = (psi as KtWhenEntry).arrow?.prevLeaf()
            !(leafBeforeArrow is PsiWhiteSpace && leafBeforeArrow.textContains('\n'))
        } else {
            false
        }

    private fun ASTNode.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        val codeLeaf = if (isCodeLeaf()) {
            this
        } else {
            prevCodeLeaf()
        }
        return codeLeaf?.takeIf { it.elementType == ElementType.COMMA }
    }

    private fun containsLineBreakInLeavesRange(from: PsiElement, to: PsiElement): Boolean {
        var leaf: PsiElement? = from
        while (leaf != null && !leaf.isEquivalentTo(to)) {
            if (leaf.textContains('\n')) {
                return true
            }
            leaf = leaf.nextLeaf(skipEmptyElements = false)
        }
        return leaf?.textContains('\n') ?: false
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

        public val TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
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
                defaultAndroidValue = false,
            )

        @Deprecated(
            message = "Marked for removal in KtLint 0.49",
            replaceWith = ReplaceWith("TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY"),
        )
        @Suppress("ktlint:experimental:property-naming")
        public val allowTrailingCommaProperty: EditorConfigProperty<Boolean> =
            TRAILING_COMMA_ON_DECLARATION_SITE_PROPERTY

        private val TYPES_ON_DECLARATION_SITE = TokenSet.create(
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
