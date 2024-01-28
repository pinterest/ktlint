package com.pinterest.ktlint.ruleset.standard.rules

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
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

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
@SinceKtlint("1.0", EXPERIMENTAL)
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
    ),
    Rule.Experimental {
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
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType == BLOCK && it.treeParent.elementType == FUN }
            ?.let { visitFunctionBody(node, autoCorrect, emit) }
    }

    private fun visitFunctionBody(
        block: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(block.elementType == BLOCK)
        block
            .takeIf { it.containingOnly(RETURN) }
            ?.takeUnless { it.countReturnKeywords() > 1 }
            ?.findChildByType(RETURN)
            ?.findChildByType(RETURN_KEYWORD)
            ?.nextSibling { !it.isWhiteSpace() }
            ?.let { codeSibling ->
                emit(block.startOffset, "Function body should be replaced with body expression", true)
                if (autoCorrect) {
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
                if (autoCorrect) {
                    with(block.treeParent) {
                        // Remove whitespace before block
                        block
                            .prevSibling()
                            .takeIf { it.isWhiteSpace() }
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
            children()
                .filterNot { it.elementType == LBRACE || it.elementType == RBRACE || it.isWhiteSpace() }
                .singleOrNull()
                ?.elementType

    private fun ASTNode.countReturnKeywords() =
        leavesInClosedRange(this.firstChildLeafOrSelf(), this.lastChildLeafOrSelf())
            .count { it.elementType == RETURN_KEYWORD }

    private fun ASTNode.createUnitTypeReference() =
        PsiFileFactory
            .getInstance(psi.project)
            .createFileFromText(KotlinLanguage.INSTANCE, "fun foo(): Unit {}")
            .getChildOfType<KtScript>()
            ?.getChildOfType<KtBlockExpression>()
            ?.getChildOfType<KtFunction>()
            ?.getChildOfType<KtTypeReference>()
            ?.node!!
}

public val FUNCTION_EXPRESSION_BODY_RULE_ID: RuleId = FunctionExpressionBodyRule().ruleId
