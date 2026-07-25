package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_END
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_START
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indent
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.ruleset.standard.StandardRule
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
                ?.prevLeaf
                ?.takeIf { !it.isWhiteSpaceWithNewline }
                ?.let {
                    // It cannot be autocorrected as it might depend on the situation and code style what is
                    // preferred.
                    emit(
                        node.startOffset,
                        "A KDoc comment after any other element on the same line must be separated by a new line",
                        false,
                    )
                }

            node
                .findChildByType(KDOC_END)
                ?.nextLeaf
                ?.takeIf { !it.isWhiteSpaceWithNewline }
                ?.let { nextLeaf ->
                    emit(nextLeaf.startOffset, "A KDoc comment may not be followed by any other element on that same line", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceAfterMe(node.indent)
                        }
                }
        }
    }
}

public val KDOC_WRAPPING_RULE_ID: RuleId = KdocWrappingRule().ruleId
