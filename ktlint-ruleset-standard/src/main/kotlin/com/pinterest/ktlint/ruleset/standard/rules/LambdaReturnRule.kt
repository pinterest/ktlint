package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LABEL_QUALIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RETURN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * [Kotlin lang documentation](https://kotlinlang.org/docs/coding-conventions.html#returns-in-a-lambda):
 *
 * Do not use a labeled return for the last statement in a lambda.
 * ```
 * foo {
 *     doSomething()
 *     return@foo "value"
 * }
 * ```
 */
@SinceKtlint("2.0", EXPERIMENTAL)
public class LambdaReturnRule :
    StandardRule(
        id = "lambda-return",
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ),
    RuleV2.Experimental {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        maxLineLength = editorConfig.maxLineLength()
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == FUNCTION_LITERAL }
            ?.findChildByType(BLOCK)
            ?.children()
            ?.lastOrNull { it.elementType == RETURN }
            ?.takeIf { it.nextCodeSibling == null }
            ?.let { lastReturn -> visitReturnStatement(lastReturn, emit) }
    }

    private fun visitReturnStatement(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == RETURN)

        node
            .findChildByType(LABEL_QUALIFIER)
            ?.siblings()
            ?.dropWhile { it.isWhiteSpace }
            ?.takeIf { it.count() > 0 }
            ?.let { siblings ->
                emit(node.startOffset, "Unnecessary return", true)
                    .ifAutocorrectAllowed {
                        val parent = node.parent!!
                        siblings
                            // The remaining siblings are moved to before the RETURN sibling. Without collecting the remaining siblings
                            // first into a list, the RETURN sibling itself would also be tried to be moved which results in a failure.
                            .toList()
                            .forEach { sibling -> parent.addChild(sibling, node) }
                        node.remove()
                    }
            }
    }
}

public val LAMBDA_RETURN_RULE_ID: RuleId = LambdaReturnRule().ruleId
