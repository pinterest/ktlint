package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_MARKDOWN_LINK
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACKET
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no extra spaces around square brackets.
 *
 * See https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace
 */
@SinceKtlint("1.2", EXPERIMENTAL)
@SinceKtlint("1.8", STABLE)
public class SpacingAroundSquareBracketsRule : StandardRule("square-brackets-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == LBRACKET || node.elementType == RBRACKET) {
            val prevLeaf = node.prevLeaf
            val nextLeaf = node.nextLeaf
            val spacingBefore =
                when (node.parent?.elementType) {
                    KDOC_MARKDOWN_LINK -> {
                        // Allow:
                        //     /**
                        //      * @see [Foo] for more information,
                        //      */
                        //     fun foo() {}
                        false
                    }

                    COLLECTION_LITERAL_EXPRESSION, DESTRUCTURING_DECLARATION -> {
                        // Allow:
                        //     @Foo(
                        //        fooBar = ["foo", "bar"],
                        //        fooBaz = [
                        //            "foo"
                        //        ]
                        // Disallow:
                        //     @Foo(fooBar = ["foo", "bar" ])
                        node.elementType == RBRACKET && prevLeaf.isWhiteSpaceWithoutNewline
                    }

                    else -> {
                        prevLeaf.isWhiteSpaceWithoutNewline
                    }
                }
            val spacingAfter =
                // Allow:
                //      val foo = bar[
                //         1,
                //         baz
                //     ]
                // and
                //     @Foo(
                //        fooBar = ["foo", "bar"],
                //        fooBaz = [
                //            "foo"
                //        ]
                // Disallow:
                //     @Foo(fooBar = [ "foo", "bar"])
                node.elementType == LBRACKET && nextLeaf.isWhiteSpaceWithoutNewline
            when {
                spacingBefore && spacingAfter -> {
                    emit(node.startOffset, "Unexpected spacing around '${node.text}'", true)
                        .ifAutocorrectAllowed {
                            prevLeaf?.remove()
                            nextLeaf?.remove()
                        }
                }

                spacingBefore -> {
                    emit(prevLeaf!!.startOffset, "Unexpected spacing before '${node.text}'", true)
                        .ifAutocorrectAllowed { prevLeaf.remove() }
                }

                spacingAfter -> {
                    emit(node.startOffset + 1, "Unexpected spacing after '${node.text}'", true)
                        .ifAutocorrectAllowed { nextLeaf!!.remove() }
                }
            }
        }
    }
}

public val SPACING_AROUND_SQUARE_BRACKETS_RULE_ID: RuleId = SpacingAroundSquareBracketsRule().ruleId
