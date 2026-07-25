package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.BLOCK
import io.github.ktlint.core.rule.engine.core.api.ElementType.CATCH
import io.github.ktlint.core.rule.engine.core.api.ElementType.FINALLY
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.TRY
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Checks spacing and wrapping of try-catch-finally.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class TryCatchFinallySpacingRule :
    StandardRule(
        id = "try-catch-finally-spacing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    RuleV2.OfficialCodeStyle {
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
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
        if (node.isPartOfComment && node.parent?.elementType in TRY_CATCH_FINALLY_TOKEN_SET) {
            emit(node.startOffset, "No comment expected at this location", false)
            return
        }
        when (node.elementType) {
            BLOCK -> {
                visitBlock(node, emit)
            }

            CATCH, FINALLY -> {
                visitClause(node, emit)
            }
        }
    }

    private fun visitBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.parent?.elementType !in TRY_CATCH_FINALLY_TOKEN_SET) {
            return
        }

        node
            .findChildByType(LBRACE)!!
            .let { lbrace ->
                val nextSibling = lbrace.nextSibling { !it.isPartOfComment }!!
                if (!nextSibling.text.startsWith("\n")) {
                    emit(lbrace.startOffset + 1, "Expected a newline after '{'", true)
                        .ifAutocorrectAllowed {
                            lbrace.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(node))
                        }
                }
            }

        node
            .findChildByType(RBRACE)!!
            .let { rbrace ->
                val prevSibling = rbrace.prevSibling { !it.isPartOfComment }!!
                if (!prevSibling.text.startsWith("\n")) {
                    emit(rbrace.startOffset, "Expected a newline before '}'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                        }
                }
            }
    }

    private fun visitClause(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prevLeaf = node.prevLeaf { !it.isPartOfComment }!!
        if (prevLeaf.text != " ") {
            emit(node.startOffset, "A single space is required before '${node.elementTypeName()}'", true)
                .ifAutocorrectAllowed {
                    node.upsertWhitespaceBeforeMe(" ")
                }
        }
    }

    private fun ASTNode.elementTypeName() = elementType.toString().lowercase()

    private companion object {
        val TRY_CATCH_FINALLY_TOKEN_SET = TokenSet.create(TRY, CATCH, FINALLY)
    }
}

public val TRY_CATCH_RULE_ID: RuleId = TryCatchFinallySpacingRule().ruleId
