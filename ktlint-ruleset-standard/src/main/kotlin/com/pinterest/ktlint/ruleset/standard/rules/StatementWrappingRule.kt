package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

@SinceKtlint("0.50", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class StatementWrappingRule :
    StandardRule(
        "statement-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
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
            BLOCK ->
                if (node.treeParent.elementType == FUNCTION_LITERAL) {
                    // LBRACE and RBRACE are outside of BLOCK
                    visitBlock(node.treeParent, emit, autoCorrect)
                } else {
                    visitBlock(node, emit, autoCorrect)
                }

            CLASS_BODY, WHEN ->
                visitBlock(node, emit, autoCorrect)

            SEMICOLON ->
                visitSemiColon(node, autoCorrect, emit)
        }
    }

    private fun visitBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeUnless {
                // Allow
                //     val foo = {}
                it.isBlockWithoutStatements
            }?.takeUnless {
                // Allow
                //     val foo = { /* no-op */ }
                //     val foo = { a, b -> println(a + b) }
                it.isFunctionLiteralWithSingleStatementOnSingleLine
            }?.takeUnless {
                // Allow
                //     enum class FooBar { FOO, BAR }
                // or
                //     /** Some comment */
                //     enum class FooBar { FOO, BAR }
                it.treeParent.isEnumClassOnSingleLine
            }?.findChildByType(LBRACE)
            ?.applyIf(node.isFunctionLiteralWithParameterList) {
                // Allow:
                // val foobar =
                //     foo { bar ->
                //         doSomething()
                //     }
                node.findChildByType(ARROW)
            }?.let { lbraceOrArrow ->
                val nextCodeLeaf = lbraceOrArrow.nextCodeLeaf()
                if (nextCodeLeaf != null && noNewLineInClosedRange(lbraceOrArrow, nextCodeLeaf)) {
                    emit(nextCodeLeaf.startOffset, "Missing newline after '${lbraceOrArrow.text}'", true)
                    if (autoCorrect) {
                        if (node.elementType == WHEN) {
                            lbraceOrArrow.upsertWhitespaceAfterMe(lbraceOrArrow.indentAsChild)
                        } else {
                            lbraceOrArrow.upsertWhitespaceAfterMe(lbraceOrArrow.indentAsSibling)
                        }
                    }
                }

                node
                    .findChildByType(RBRACE)
                    ?.let { rbrace ->
                        val prevCodeLeaf = rbrace.prevCodeLeaf()
                        if (prevCodeLeaf != null && noNewLineInClosedRange(prevCodeLeaf, rbrace)) {
                            emit(rbrace.startOffset, "Missing newline before '}'", true)
                            if (autoCorrect) {
                                rbrace.upsertWhitespaceBeforeMe(rbrace.indentAsParent)
                            }
                        }
                    }
            }
    }

    private inline val ASTNode.isBlockWithoutStatements: Boolean
        get() =
            RBRACE ==
                findChildByType(LBRACE)
                    ?.nextCodeLeaf()
                    ?.elementType

    private inline val ASTNode.isFunctionLiteralWithParameterList: Boolean
        get() =
            elementType == FUNCTION_LITERAL &&
                findChildByType(VALUE_PARAMETER_LIST) != null

    private inline val ASTNode.isFunctionLiteralWithSingleStatementOnSingleLine: Boolean
        get() =
            takeIf { elementType == FUNCTION_LITERAL }
                ?.takeUnless { it.textContains('\n') }
                ?.findChildByType(BLOCK)
                ?.children()
                ?.count { it.elementType != VALUE_PARAMETER_LIST && it.elementType != ARROW }
                ?.let { count -> count <= 1 }
                ?: false

    private inline val ASTNode.isEnumClassOnSingleLine: Boolean
        get() =
            if (psi.isEnumClass) {
                val lastChildLeaf = lastChildLeafOrSelf()
                // Ignore the leading comment
                noNewLineInClosedRange(firstCodeLeafOrNull!!, lastChildLeaf)
            } else {
                false
            }

    private inline val ASTNode.firstCodeLeafOrNull: ASTNode?
        get() =
            // Skip the comment on top of the node by getting modifier list
            findChildByType(MODIFIER_LIST)
                ?.children()
                ?.dropWhile {
                    // Ignore annotations placed on separate lines above the node
                    it.elementType == ANNOTATION_ENTRY || it.isWhiteSpace()
                }?.firstOrNull()
                ?.firstChildLeafOrSelf()

    private inline val PsiElement.isEnumClass: Boolean
        get() = (this as? KtClass)?.isEnum() ?: false

    private inline val ASTNode.indentAsChild: String
        get() = indent().plus(indentConfig.indent)

    private inline val ASTNode.indentAsSibling: String
        get() = treeParent.indent().plus(indentConfig.indent)

    private inline val ASTNode.indentAsParent: String
        get() = treeParent.indent()

    private fun visitSemiColon(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val previousCodeLeaf = node.prevCodeLeaf()?.lastChildLeafOrSelf() ?: return
        val nextCodeLeaf = node.nextCodeLeaf()?.firstChildLeafOrSelf() ?: return
        if (previousCodeLeaf.treeParent.elementType == ElementType.ENUM_ENTRY && nextCodeLeaf.elementType == RBRACE) {
            // Allow
            // enum class INDEX2 { ONE, TWO, THREE; }
            return
        }
        if (noNewLineInClosedRange(previousCodeLeaf, nextCodeLeaf)) {
            emit(node.startOffset + 1, """Missing newline after '${node.text}'""", true)
            if (autoCorrect) {
                node.upsertWhitespaceAfterMe(previousCodeLeaf.indent())
            }
        }
    }
}

public val STATEMENT_WRAPPING_RULE_ID: RuleId = StatementWrappingRule().ruleId
