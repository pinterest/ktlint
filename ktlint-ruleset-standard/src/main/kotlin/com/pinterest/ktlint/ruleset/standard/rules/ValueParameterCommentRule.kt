package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * The AST allows comments to be placed anywhere. This however can lead to code which is unnecessarily hard to read. Or, it makes
 * development of rules unnecessarily complex.
 *
 * This rule is based on the DiscouragedCommentLocationRule which is split in several distinct rules. In this way it becomes more clear why
 * another rule depends on this rule.
 */
@SinceKtlint("1.1", STABLE)
public class ValueParameterCommentRule : StandardRule("value-parameter-comment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isPartOfComment() && node.treeParent.elementType in valueParameterTokenSet) {
            when (node.elementType) {
                EOL_COMMENT, BLOCK_COMMENT -> {
                    if (node.treeParent.elementType == VALUE_PARAMETER) {
                        // Disallow:
                        //     class Foo(
                        //         bar /* some comment */ = "bar"
                        //     )
                        // or
                        //     class Foo(
                        //         bar =
                        //            // some comment
                        //            "bar"
                        //     )
                        emit(
                            node.startOffset,
                            "A (block or EOL) comment inside or on same line after a 'value_parameter' is not allowed. It may be placed " +
                                "on a separate line above.",
                            false,
                        )
                    } else if (node.treeParent.elementType == VALUE_PARAMETER_LIST) {
                        if (node.prevLeaf().isWhiteSpaceWithNewline()) {
                            // Allow:
                            //     class Foo(
                            //         // some comment
                            //         bar = "bar"
                            //     )
                        } else {
                            // Disallow
                            //     class Foo(
                            //         val bar1: Bar, // some comment 1
                            //         // some comment 2
                            //         val bar2: Bar,
                            //     )
                            // It is not clear whether "some comment 2" belongs to bar1 as a continuation of "some comment 1" or that it belongs to
                            // bar2. Note both comments are direct children of the value_parameter_list.
                            emit(
                                node.startOffset,
                                "A comment in a 'value_parameter_list' is only allowed when placed on a separate line",
                                false,
                            )
                        }
                    }
                }

                KDOC -> {
                    if (node.treeParent.elementType == VALUE_PARAMETER) {
                        if (node == node.treeParent.firstChildNode) {
                            // Allow
                            //      class Foo(
                            //         /** some comment */
                            //         val bar: Bar,
                            //     )
                        } else {
                            // Disallow a kdoc inside a VALUE_PARAMETER.
                            //      class Foo(
                            //         val bar:
                            //             /** some comment */
                            //             Bar,
                            //     )
                            // or
                            //      class Foo(
                            //         val bar: Bar /** some comment */
                            //     )
                            emit(
                                node.startOffset,
                                "A kdoc in a 'value_parameter' is only allowed when placed on a new line before this element",
                                false,
                            )
                        }
                    } else {
                        emit(
                            node.startOffset,
                            "A KDoc is not allowed inside a 'value_parameter_list' when not followed by a property",
                            false,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val valueParameterTokenSet = TokenSet.create(VALUE_PARAMETER, VALUE_PARAMETER_LIST)
    }
}

public val VALUE_PARAMETER_COMMENT_RULE_ID: RuleId = ValueParameterCommentRule().ruleId
