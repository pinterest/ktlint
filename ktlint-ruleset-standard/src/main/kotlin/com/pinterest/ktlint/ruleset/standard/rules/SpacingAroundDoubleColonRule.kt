package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALLABLE_REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLONCOLON
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.37", STABLE)
public class SpacingAroundDoubleColonRule : StandardRule("double-colon-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == COLONCOLON) {
            val prevLeaf = node.prevLeaf
            val nextLeaf = node.nextLeaf

            var removeSingleWhiteSpace = false
            val spacingBefore =
                when {
                    node.isPartOf(CLASS_LITERAL_EXPRESSION) && prevLeaf.isWhiteSpace20 -> {
                        true
                    }

                    // Clazz::class
                    node.isPartOf(CALLABLE_REFERENCE_EXPRESSION) && prevLeaf.isWhiteSpace20 -> {
                        // String::length, ::isOdd
                        if (node.prevSibling20 == null) { // compose(length, ::isOdd), val predicate = ::isOdd
                            removeSingleWhiteSpace = true
                            prevLeaf
                                .takeIf { it.isWhiteSpaceWithoutNewline20 }
                                ?.let { it.textLength > 1 }
                                ?: false
                        } else { // String::length, List<String>::isEmpty
                            prevLeaf.isWhiteSpaceWithoutNewline20
                        }
                    }

                    else -> {
                        false
                    }
                }
            val spacingAfter = nextLeaf.isWhiteSpace20
            when {
                spacingBefore && spacingAfter -> {
                    emit(node.startOffset, "Unexpected spacing around \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            prevLeaf!!.removeSelf(removeSingleWhiteSpace)
                            nextLeaf!!.remove()
                        }
                }

                spacingBefore -> {
                    emit(prevLeaf!!.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            prevLeaf.removeSelf(removeSingleWhiteSpace)
                        }
                }

                spacingAfter -> {
                    emit(nextLeaf!!.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                        .ifAutocorrectAllowed { nextLeaf.remove() }
                }
            }
        }
    }

    private fun ASTNode.removeSelf(removeSingleWhiteSpace: Boolean) {
        if (removeSingleWhiteSpace) {
            replaceTextWith(text.substring(0, textLength - 1))
        } else {
            this.remove()
        }
    }
}

public val SPACING_AROUND_DOUBLE_COLON_RULE_ID: RuleId = SpacingAroundDoubleColonRule().ruleId
