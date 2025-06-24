package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RETURN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RETURN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.KtlintKotlinCompiler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html#functions):
 * Prefer using an expression body for functions with the body consisting of a single expression.
 * ```
 * override fun toString(): String {
 *     return "Hey"
 * }
 * >
 * override fun toString(): String = "Hey"
 * ```
 *
 * [Android Kotlin styleguide](https://developer.android.com/kotlin/style-guide#expression_functions):
 *
 * When a function contains only a single expression it can be represented as an [expression function]
 * ```
 * override fun toString(): String {
 *     return "Hey"
 * }
 * >
 * override fun toString(): String {
 *     return "Hey"
 * }
 * ```
 */
@SinceKtlint("1.0", STABLE)
public class FunctionExpressionBodyRule :
    StandardRule(
        id = "function-expression-body",
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == BLOCK && it.treeParent.elementType == FUN }
            ?.let { visitFunctionBody(node, emit) }
    }

    private fun visitFunctionBody(
        block: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(block.elementType == BLOCK)
        block
            .takeIf { it.containingOnly(RETURN) }
            ?.takeUnless { it.countReturnKeywords() > 1 }
            ?.findChildByType(RETURN)
            ?.findChildByType(RETURN_KEYWORD)
            ?.nextSibling { !it.isWhiteSpace20 }
            ?.let { codeSibling ->
                emit(block.startOffset, "Function body should be replaced with body expression", true)
                    .ifAutocorrectAllowed {
                        with(block.treeParent) {
                            // Insert the code sibling before the block
                            addChild(LeafPsiElement(EQ, "="), block)
                            addChild(PsiWhiteSpaceImpl(" "), block)
                            addChild(codeSibling, block)
                            // Remove old (and now empty block)
                            removeChild(block)
                        }
                    }
            }
        block
            .takeIf { it.containingOnly(THROW) }
            ?.findChildByType(THROW)
            ?.let { throwNode ->
                emit(block.startOffset, "Function body should be replaced with body expression", true)
                    .ifAutocorrectAllowed {
                        with(block.treeParent) {
                            // Remove whitespace before block
                            block
                                .prevSibling20
                                .takeIf { it.isWhiteSpace20 }
                                ?.let { removeChild(it) }
                            if (findChildByType(TYPE_REFERENCE) == null) {
                                // Insert Unit as return type as otherwise a compilation error results
                                addChild(LeafPsiElement(COLON, ":"), block)
                                addChild(PsiWhiteSpaceImpl(" "), block)
                                addChild(createUnitTypeReference(), block)
                            }
                            addChild(PsiWhiteSpaceImpl(" "), block)
                            addChild(LeafPsiElement(EQ, "="), block)
                            addChild(PsiWhiteSpaceImpl(" "), block)
                            addChild(throwNode, block)
                            // Remove old (and now empty block)
                            removeChild(block)
                        }
                    }
            }
    }

    private fun ASTNode.containingOnly(iElementType: IElementType) =
        iElementType ==
            children20
                .filterNot { it.elementType == LBRACE || it.elementType == RBRACE || it.isWhiteSpace20 }
                .singleOrNull()
                ?.elementType

    private fun ASTNode.countReturnKeywords() =
        leavesInClosedRange(firstChildLeafOrSelf20, lastChildLeafOrSelf20)
            .count { it.elementType == RETURN_KEYWORD }

    private fun createUnitTypeReference() =
        KtlintKotlinCompiler
            .createASTNodeFromText("fun foo(): Unit {}")
            ?.findChildByType(FUN)
            ?.findChildByType(TYPE_REFERENCE)
            ?: throw IllegalStateException("Can not create function with unit type reference")
}

public val FUNCTION_EXPRESSION_BODY_RULE_ID: RuleId = FunctionExpressionBodyRule().ruleId
