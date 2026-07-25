package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUN
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_REFERENCE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.nextCodeSibling
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.7", STABLE)
public class NoUnitReturnRule : StandardRule("no-unit-return") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == TYPE_REFERENCE &&
            node.text == "Unit" &&
            node.parent?.elementType == FUN &&
            node.nextCodeSibling?.firstChildNode?.elementType == LBRACE
        ) {
            emit(node.startOffset, "Unnecessary \"Unit\" return type", true)
                .ifAutocorrectAllowed {
                    node
                        .parent
                        ?.findChildByType(COLON)
                        ?.let { colonNode ->
                            // Remove space after colon when not followed by Unit node
                            node
                                .nextLeaf
                                .takeIf { it.isWhiteSpace }
                                ?.takeIf { it.nextLeaf != node }
                                ?.remove()
                            colonNode.remove()
                        }
                    node
                        .prevLeaf
                        .takeIf { it.isWhiteSpace }
                        ?.takeIf { it.prevLeaf?.elementType != COLON }
                        ?.remove()
                    node.remove()
                }
        }
    }
}

public val NO_UNIT_RETURN_RULE_ID: RuleId = NoUnitReturnRule().ruleId
