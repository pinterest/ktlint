package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.INDICES
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isCodeLeaf
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevCodeSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import kotlin.properties.Delegates
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtPsiFactory

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
                ruleId = "standard:wrapping",
                loadOnlyWhenOtherRuleIsLoaded = true,
                runOnlyWhenOtherRuleIsEnabled = true,
            ),
            VisitorModifier.RunAsLateAsPossible,
        ),
    ),
    UsesEditorConfigProperties {

    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        TRAILING_COMMA_ON_CALL_SITE_PROPERTY,
    )

    private var allowTrailingCommaOnCallSite by Delegates.notNull<Boolean>()

    private fun ASTNode.isTrailingCommaAllowed() =
        elementType in TYPES_ON_CALL_SITE && allowTrailingCommaOnCallSite

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        allowTrailingCommaOnCallSite = editorConfigProperties.getEditorConfigValue(TRAILING_COMMA_ON_CALL_SITE_PROPERTY)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            COLLECTION_LITERAL_EXPRESSION -> visitCollectionLiteralExpression(node, autoCorrect, emit)
            INDICES -> visitIndices(node, autoCorrect, emit)
            TYPE_ARGUMENT_LIST -> visitTypeList(node, autoCorrect, emit)
            VALUE_ARGUMENT_LIST -> visitValueList(node, autoCorrect, emit)
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
        val trailingCommaNode = prevLeaf?.findPreviousTrailingCommaNodeOrNull()
        val trailingCommaState = when {
            this.isMultiline() -> if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
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
                    val prevPsi = inspectNode.prevCodeSibling()!!.psi
                    val parentPsi = prevPsi.parent
                    val psiFactory = KtPsiFactory(prevPsi)
                    parentPsi.addAfter(psiFactory.createComma(), prevPsi)
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
            ?.nextSibling { it.isWhiteSpaceWithNewline() }

    private fun ASTNode.hasAtLeastOneArgument() =
        children().any { it.elementType == VALUE_ARGUMENT }

    private fun ASTNode.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
        val codeLeaf = if (isCodeLeaf()) {
            this
        } else {
            prevCodeLeaf()
        }
        return codeLeaf?.takeIf { it.elementType == ElementType.COMMA }
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
                type = PropertyType.LowerCasingPropertyType(
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
                defaultAndroidValue = false,
            )

        @Deprecated(
            message = "Marked for removal in KtLint 0.49",
            replaceWith = ReplaceWith("TRAILING_COMMA_ON_CALL_SITE_PROPERTY"),
        )
        @Suppress("ktlint:experimental:property-naming")
        public val allowTrailingCommaOnCallSiteProperty: EditorConfigProperty<Boolean> =
            TRAILING_COMMA_ON_CALL_SITE_PROPERTY

        private val TYPES_ON_CALL_SITE = TokenSet.create(
            COLLECTION_LITERAL_EXPRESSION,
            INDICES,
            TYPE_ARGUMENT_LIST,
            VALUE_ARGUMENT_LIST,
        )
    }
}
