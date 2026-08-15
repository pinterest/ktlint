package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isRoot
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.leavesForwardsIncludingSelf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("2.0", EXPERIMENTAL)
public class NoBlankLineAtStartOfFileRule :
    StandardRule(id = "no-blank-line-at-start-of-file"),
    RuleV2.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isRoot) {
            node
                .firstChildLeafOrSelf
                .leavesForwardsIncludingSelf
                .takeWhile { it.isWhiteSpace || it.textLength == 0 }
                .filter { it.isWhiteSpace }
                .forEach {
                    emit(it.startOffset, "Unexpected whitespace at start of file", true)
                        .ifAutocorrectAllowed { it.remove() }
                }
            stopTraversalOfAST()
        }
    }
}

public val NO_BLANK_LINE_AT_START_OF_FILE_RULE_ID: RuleId = NoBlankLineAtStartOfFileRule().ruleId
