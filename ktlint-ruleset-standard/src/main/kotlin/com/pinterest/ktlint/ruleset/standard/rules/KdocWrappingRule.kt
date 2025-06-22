package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_END
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_START
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Checks external wrapping of KDoc comment. Wrapping inside the KDoc comment is not altered.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class KdocWrappingRule :
    StandardRule(
        id = "kdoc-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == KDOC) {
            node
                .findChildByType(KDOC_START)
                ?.prevLeaf()
                ?.takeIf { isNonIndentLeafOnSameLine(it) }
                ?.let {
                    // It can not be autocorrected as it might depend on the situation and code style what is
                    // preferred.
                    emit(
                        node.startOffset,
                        "A KDoc comment after any other element on the same line must be separated by a new line",
                        false,
                    )
                }

            node
                .findChildByType(KDOC_END)
                ?.nextLeaf()
                ?.takeIf { isNonIndentLeafOnSameLine(it) }
                ?.let { nextLeaf ->
                    emit(nextLeaf.startOffset, "A KDoc comment may not be followed by any other element on that same line", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceAfterMe(node.indent20)
                        }
                }
        }
    }

    private fun isNonIndentLeafOnSameLine(it: ASTNode) = it.elementType != WHITE_SPACE || !it.textContains('\n')
}

public val KDOC_WRAPPING_RULE_ID: RuleId = KdocWrappingRule().ruleId
