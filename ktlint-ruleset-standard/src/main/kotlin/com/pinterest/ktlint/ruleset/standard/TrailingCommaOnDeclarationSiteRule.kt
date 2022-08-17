package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.containsLineBreakInRange
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import kotlin.properties.Delegates
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFunctionLiteral
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
                ruleId = "standard:indent",
                loadOnlyWhenOtherRuleIsLoaded = true,
                runOnlyWhenOtherRuleIsEnabled = true,
            ),
            VisitorModifier.RunAsLateAsPossible,
        ),
    ),
    UsesEditorConfigProperties {

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        allowTrailingCommaProperty,
    )

    private var allowTrailingComma by Delegates.notNull<Boolean>()

    private fun ASTNode.isTrailingCommaAllowed() =
        elementType in TYPES_ON_DECLARATION_SITE && allowTrailingComma

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        allowTrailingComma = editorConfigProperties.getEditorConfigValue(allowTrailingCommaProperty)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            ElementType.CLASS -> visitClass(node, emit, autoCorrect)
            ElementType.DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node, autoCorrect, emit)
            ElementType.FUNCTION_LITERAL -> visitFunctionLiteral(node, autoCorrect, emit)
            ElementType.TYPE_PARAMETER_LIST -> visitTypeList(node, autoCorrect, emit)
            ElementType.VALUE_PARAMETER_LIST -> visitValueList(node, autoCorrect, emit)
            ElementType.WHEN_ENTRY -> visitWhenEntry(node, autoCorrect, emit)
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
            .lastOrNull { it.elementType == ElementType.ARROW }
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
        if (node.treeParent.elementType != ElementType.FUNCTION_LITERAL) {
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
            .first { it.elementType == ElementType.ARROW }
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

        return lastTwoEnumEntries.count() == 2 && lastTwoEnumEntries[0].lineNumber() == lastTwoEnumEntries[1].lineNumber()
    }

    private fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode,
        isTrailingCommaAllowed: Boolean,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val prevLeaf = inspectNode.prevLeaf()
        val trailingCommaNode = prevLeaf.findPreviousTrailingCommaNodeOrNull()
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
                                    if (lastNodeBeforeArrow.elementType == WHITE_SPACE) {
                                        (lastNodeBeforeArrow as LeafPsiElement).rawReplaceWithText(parentIndent)
                                    } else {
                                        (lastNodeBeforeArrow as LeafPsiElement).upsertWhitespaceAfterMe(parentIndent)
                                    }
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

                        if (inspectNode.treeParent.elementType == ElementType.ENUM_ENTRY) {
                            with(KtPsiFactory(prevNode.psi)) {
                                val parentIndent = (prevNode.psi.parent.prevLeaf() as? PsiWhiteSpace)?.text ?: "\n"
                                val newline = createWhiteSpace(parentIndent)
                                val enumEntry = inspectNode.treeParent.psi
                                enumEntry.apply {
                                    add(newline)
                                    removeChild(inspectNode)
                                    parent.addAfter(createSemicolon(), this)
                                }
                            }
                        }

                        val comma = KtPsiFactory(prevNode.psi).createComma()
                        prevNode.psi.parent.addAfter(comma, prevNode.psi)
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
            .last { it.elementType == ElementType.WHEN_ENTRY }
            .lineIndent()

    private fun isMultiline(element: PsiElement): Boolean = when {
        element.parent is KtFunctionLiteral -> isMultiline(element.parent)
        element is KtFunctionLiteral -> containsLineBreakInRange(element.valueParameterList!!, element.arrow!!)
        element is KtWhenEntry -> containsLineBreakInRange(element.firstChild, element.arrow!!)
        element is KtDestructuringDeclaration -> containsLineBreakInRange(element.lPar!!, element.rPar!!)
        element is KtValueArgumentList && element.children.size == 1 && element.anyDescendantOfType<KtCollectionLiteralExpression>() -> {
            // special handling for collection literal
            // @Annotation([
            //    "something",
            // ])
            val lastChild = element.collectDescendantsOfType<KtCollectionLiteralExpression>().last()
            containsLineBreakInLeavesRange(lastChild.rightBracket!!, element.rightParenthesis!!)
        }
        else -> element.textContains('\n')
    }

    private fun ASTNode.addNewLineBeforeArrowInWhen() =
        if (psi is KtWhenEntry) {
            val leafBeforeArrow = (psi as KtWhenEntry).arrow?.prevLeaf()
            !(leafBeforeArrow is PsiWhiteSpace && leafBeforeArrow.textContains('\n'))
        } else {
            false
        }

    private fun ASTNode?.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        var node = this
        while (node?.isIgnorable() == true) {
            node = node.prevLeaf()
        }
        return if (node?.elementType == ElementType.COMMA) {
            node
        } else {
            null
        }
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

    private fun ASTNode.isIgnorable(): Boolean =
        elementType == ElementType.WHITE_SPACE ||
            elementType == ElementType.EOL_COMMENT ||
            elementType == ElementType.BLOCK_COMMENT

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

        internal const val ALLOW_TRAILING_COMMA_NAME = "ij_kotlin_allow_trailing_comma"
        private const val ALLOW_TRAILING_COMMA_DESCRIPTION = "Defines whether a trailing comma (or no trailing comma)" +
            "should be enforced on the defining side," +
            "e.g. parameter-list, type-argument-list, lambda-value-parameters, enum-entries, etc."

        private val BOOLEAN_VALUES_SET = setOf("true", "false")

        // TODO: Rename property to trailingCommaOnDeclarationSite. The word 'allow' is misleading as the comma is
        //       enforced when the property is enabled and prohibited when disabled.
        public val allowTrailingCommaProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    ALLOW_TRAILING_COMMA_NAME,
                    ALLOW_TRAILING_COMMA_DESCRIPTION,
                    PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    BOOLEAN_VALUES_SET,
                ),
                defaultValue = false,
            )

        private val TYPES_ON_DECLARATION_SITE = TokenSet.create(
            ElementType.CLASS,
            ElementType.DESTRUCTURING_DECLARATION,
            ElementType.FUNCTION_LITERAL,
            ElementType.FUNCTION_TYPE,
            ElementType.TYPE_PARAMETER_LIST,
            ElementType.VALUE_PARAMETER_LIST,
            ElementType.WHEN_ENTRY,
        )
    }
}
