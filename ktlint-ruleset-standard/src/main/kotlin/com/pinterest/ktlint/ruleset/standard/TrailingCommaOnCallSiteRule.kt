package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.containsLineBreakInRange
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import kotlin.properties.Delegates
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

/**
 * Linting trailing comma for call site.
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html#trailing-commas)
 */
public class TrailingCommaOnCallSiteRule :
    Rule(
        id = "trailing-comma-on-call-site",
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
        allowTrailingCommaOnCallSiteProperty,
    )

    private var allowTrailingCommaOnCallSite by Delegates.notNull<Boolean>()

    private fun ASTNode.isTrailingCommaAllowed() =
        elementType in TYPES_ON_CALL_SITE && allowTrailingCommaOnCallSite

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        allowTrailingCommaOnCallSite = editorConfigProperties.getEditorConfigValue(allowTrailingCommaOnCallSiteProperty)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            ElementType.COLLECTION_LITERAL_EXPRESSION -> visitCollectionLiteralExpression(node, autoCorrect, emit)
            ElementType.INDICES -> visitIndices(node, autoCorrect, emit)
            ElementType.TYPE_ARGUMENT_LIST -> visitTypeList(node, autoCorrect, emit)
            ElementType.VALUE_ARGUMENT_LIST -> visitValueList(node, autoCorrect, emit)
            else -> Unit
        }
    }

    private fun visitCollectionLiteralExpression(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val inspectNode = node
            .children()
            .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
        )
    }

    private fun visitIndices(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val inspectNode = node
            .children()
            .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(
            inspectNode = inspectNode,
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
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
                        emit = emit,
                        isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
                        autoCorrect = autoCorrect,
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
            emit = emit,
            isTrailingCommaAllowed = node.isTrailingCommaAllowed(),
            autoCorrect = autoCorrect,
        )
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
            TrailingCommaState.EXISTS -> if (!isTrailingCommaAllowed) {
                emit(
                    trailingCommaNode!!.startOffset,
                    "Unnecessary trailing comma before \"${inspectNode.text}\"",
                    true,
                )
                if (autoCorrect) {
                    this.removeChild(trailingCommaNode)
                }
            }
            TrailingCommaState.MISSING -> if (isTrailingCommaAllowed) {
                val prevNode = inspectNode.prevCodeLeaf()!!
                emit(
                    prevNode.startOffset + prevNode.textLength,
                    "Missing trailing comma before \"${inspectNode.text}\"",
                    true,
                )
                if (autoCorrect) {
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
        internal const val ALLOW_TRAILING_COMMA_ON_CALL_SITE_NAME = "ij_kotlin_allow_trailing_comma_on_call_site"
        private const val ALLOW_TRAILING_COMMA_ON_CALL_SITE_DESCRIPTION =
            "Defines whether a trailing comma (or no trailing comma) should be enforced on the calling side," +
                "e.g. argument-list, when-entries, lambda-arguments, indices, etc."
        private val BOOLEAN_VALUES_SET = setOf("true", "false")

        // TODO: Rename property to trailingCommaOnCallSite. The word 'allow' is misleading as the comma is
        //       enforced when the property is enabled and prohibited when disabled.
        public val allowTrailingCommaOnCallSiteProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    ALLOW_TRAILING_COMMA_ON_CALL_SITE_NAME,
                    ALLOW_TRAILING_COMMA_ON_CALL_SITE_DESCRIPTION,
                    PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    BOOLEAN_VALUES_SET,
                ),
                defaultValue = false,
            )

        private val TYPES_ON_CALL_SITE = TokenSet.create(
            ElementType.COLLECTION_LITERAL_EXPRESSION,
            ElementType.INDICES,
            ElementType.TYPE_ARGUMENT_LIST,
            ElementType.VALUE_ARGUMENT_LIST,
        )
    }
}
