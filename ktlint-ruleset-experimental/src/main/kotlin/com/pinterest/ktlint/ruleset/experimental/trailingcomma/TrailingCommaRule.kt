package com.pinterest.ktlint.ruleset.experimental.trailingcomma

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.assertElementType
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.containsLineBreakInChild
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.lineCount
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import kotlin.properties.Delegates
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(FeatureInAlphaState::class)
public class TrailingCommaRule :
    Rule("trailing-comma"),
    UsesEditorConfigProperties {

    private var allowTrailingComma by Delegates.notNull<Boolean>()
    private var allowTrailingCommaOnCallSite by Delegates.notNull<Boolean>()

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        allowTrailingCommaProperty,
        allowTrailingCommaOnCallSiteProperty,
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isRoot()) {
            getEditorConfigValues(node)
            return
        }

        // Keep processing of element types in sync with Intellij Kotlin formatting settings.
        // https://github.com/JetBrains/intellij-kotlin/blob/master/formatter/src/org/jetbrains/kotlin/idea/formatter/trailingComma/util.kt
        when (node.elementType) {
            ElementType.DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node, emit, autoCorrect)
            ElementType.FUNCTION_LITERAL -> visitFunctionLiteral(node, emit, autoCorrect)
            ElementType.FUNCTION_TYPE -> visitFunctionType(node, emit, autoCorrect)
            ElementType.TYPE_PARAMETER_LIST -> visitTypeList(node, emit, autoCorrect)
            ElementType.VALUE_PARAMETER_LIST -> visitValueList(node, emit, autoCorrect)
            ElementType.WHEN_ENTRY -> visitWhenEntry(node, emit, autoCorrect)
            else -> Unit
        }
        when (node.elementType) {
            ElementType.COLLECTION_LITERAL_EXPRESSION -> visitCollectionLiteralExpression(node, emit, autoCorrect)
            ElementType.INDICES -> visitIndices(node, emit, autoCorrect)
            ElementType.TYPE_ARGUMENT_LIST -> visitTypeList(node, emit, autoCorrect)
            ElementType.VALUE_ARGUMENT_LIST -> visitValueList(node, emit, autoCorrect)
            else -> Unit
        }
    }

    private fun getEditorConfigValues(node: ASTNode) {
        val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
        allowTrailingComma = editorConfig.getEditorConfigValue(allowTrailingCommaProperty)
        allowTrailingCommaOnCallSite = editorConfig.getEditorConfigValue(allowTrailingCommaOnCallSiteProperty)
    }

    private fun visitCollectionLiteralExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.COLLECTION_LITERAL_EXPRESSION)
            .children()
            .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitDestructuringDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.DESTRUCTURING_DECLARATION)
            .children()
            .last { it.elementType == ElementType.RPAR }
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitFunctionLiteral(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.FUNCTION_LITERAL)
            .children()
            .lastOrNull { it.elementType == ElementType.ARROW }
            ?: // lambda w/o an arrow -> no arguments -> no commas
            return
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitFunctionType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.FUNCTION_TYPE)
            .firstChildNode
            .assertElementType(ElementType.VALUE_PARAMETER_LIST)
            .children()
            .last()
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitIndices(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.INDICES)
            .children()
            .last { it.elementType == ElementType.RBRACKET }
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitValueList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.treeParent.elementType != ElementType.FUNCTION_LITERAL &&
            node.treeParent.elementType != ElementType.FUNCTION_TYPE
        ) {
            val inspectNode = node
                .assertElementType(ElementType.VALUE_ARGUMENT_LIST, ElementType.VALUE_PARAMETER_LIST)
                .children()
                .last { it.elementType == ElementType.RPAR }
            node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
        }
    }

    private fun visitTypeList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val inspectNode = node
            .assertElementType(ElementType.TYPE_ARGUMENT_LIST, ElementType.TYPE_PARAMETER_LIST)
            .children()
            .first { it.elementType == ElementType.GT }
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun visitWhenEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
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
        node.reportAndCorrectTrailingCommaNodeBefore(inspectNode, emit, autoCorrect)
    }

    private fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
        inspectNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val prevLeaf = inspectNode.prevLeaf()
        val trailingCommaNode = prevLeaf.findPreviousTrailingCommaNodeOrNull()
        val trailingCommaState = when {
            isMultiline(psi) -> if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
            else -> if (trailingCommaNode != null) TrailingCommaState.REDUNDANT else TrailingCommaState.NOT_EXISTS
        }
        val isTrailingCommaAllowed = when (elementType) {
            in typesOnDeclarationSite -> allowTrailingComma
            in typesOnCallSite -> allowTrailingCommaOnCallSite
            else -> false
        }
        when (trailingCommaState) {
            TrailingCommaState.EXISTS -> if (!isTrailingCommaAllowed) {
                emit(
                    trailingCommaNode!!.psi.startOffset,
                    "Unnecessary trailing comma before \"${inspectNode.text}\"",
                    true
                )
                if (autoCorrect) {
                    this.removeChild(trailingCommaNode)
                }
            }
            TrailingCommaState.MISSING -> if (isTrailingCommaAllowed) {
                val prevNode = inspectNode.prevCodeLeaf()
                emit(
                    prevNode!!.startOffset,
                    "Missing trailing comma before \"${inspectNode.text}\"",
                    true
                )
                if (autoCorrect) {
                    val comma = KtPsiFactory(prevNode.psi).createComma()
                    this.psi.addAfter(comma, prevNode.psi)
//                    (prevNode as LeafPsiElement).rawInsertAfterMe(PsiWhiteSpace)
//                    this.replaceChild(prevNode, LeafPsiElement(ElementType.COMMA, ","))
                }
            }
            TrailingCommaState.REDUNDANT -> {
                emit(
                    trailingCommaNode!!.startOffset,
                    "Unnecessary trailing comma before \"${inspectNode.text}\"",
                    true
                )
                if (autoCorrect) {
                    this.removeChild(trailingCommaNode)
                }
            }
            TrailingCommaState.NOT_EXISTS -> Unit
        }
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

    private fun isMultiline(element: PsiElement): Boolean = when {
        element.parent is KtFunctionLiteral -> isMultiline(element.parent)

        element is KtFunctionLiteral -> element.isMultiline(
            startOffsetGetter = { valueParameterList?.startOffset },
            endOffsetGetter = { arrow?.endOffset },
        )

        element is KtWhenEntry -> element.isMultiline(
            startOffsetGetter = { startOffset },
            endOffsetGetter = { arrow?.endOffset },
        )

        element is KtDestructuringDeclaration -> element.isMultiline(
            startOffsetGetter = { lPar?.startOffset },
            endOffsetGetter = { rPar?.endOffset },
        )

        else -> element.lineCount > 1
    }

    private fun <T : PsiElement> T.isMultiline(
        startOffsetGetter: T.() -> Int?,
        endOffsetGetter: T.() -> Int?,
    ): Boolean {
        val startOffset = startOffsetGetter() ?: startOffset
        val endOffset = endOffsetGetter() ?: endOffset
        return containsLineBreakInChild(startOffset, endOffset)
    }

    private fun ASTNode.isIgnorable(): Boolean =
        elementType == ElementType.WHITE_SPACE ||
            elementType == ElementType.EOL_COMMENT ||
            elementType == ElementType.BLOCK_COMMENT

    public companion object {
        private val typesOnDeclarationSite = TokenSet.create(
            ElementType.DESTRUCTURING_DECLARATION,
            ElementType.FUNCTION_LITERAL,
            ElementType.FUNCTION_TYPE,
            ElementType.TYPE_PARAMETER_LIST,
            ElementType.VALUE_PARAMETER_LIST,
            ElementType.WHEN_ENTRY
        )

        private val typesOnCallSite = TokenSet.create(
            ElementType.COLLECTION_LITERAL_EXPRESSION,
            ElementType.INDICES,
            ElementType.TYPE_ARGUMENT_LIST,
            ElementType.VALUE_ARGUMENT_LIST
        )

        internal const val ALLOW_TRAILING_COMMA_NAME = "ij_kotlin_allow_trailing_comma"
        private const val ALLOW_TRAILING_COMMA_DESCRIPTION = "Defines whether a trailing comma (or no trailing comma)" +
            "should be enforced on the defining side," +
            "e.g. parameter-list, type-argument-list, lambda-value-parameters, enum-entries, etc."

        private val BOOLEAN_VALUES_SET = setOf("true", "false")

        public val allowTrailingCommaProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    ALLOW_TRAILING_COMMA_NAME,
                    ALLOW_TRAILING_COMMA_DESCRIPTION,
                    PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    BOOLEAN_VALUES_SET
                ),
                defaultValue = false
            )

        internal const val ALLOW_TRAILING_COMMA_ON_CALL_SITE_NAME = "ij_kotlin_allow_trailing_comma_on_call_site"
        private const val ALLOW_TRAILING_COMMA_ON_CALL_SITE_DESCRIPTION =
            "Defines whether a trailing comma (or no trailing comma)" +
                "should be enforced on the calling side," +
                "e.g. argument-list, when-entries, lambda-arguments, indices, etc."

        public val allowTrailingCommaOnCallSiteProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    ALLOW_TRAILING_COMMA_ON_CALL_SITE_NAME,
                    ALLOW_TRAILING_COMMA_ON_CALL_SITE_DESCRIPTION,
                    PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    BOOLEAN_VALUES_SET
                ),
                defaultValue = false
            )
    }
}
