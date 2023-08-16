package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * The AST allows comments to be placed anywhere. This however can lead to code which is unnecessarily hard to read.
 * Disallowing comments at certain positions in the AST makes development of a rule easier as they have not to be taken
 * into account in that rule.
 *
 * In examples below, a comment is placed between a type parameter list and the function name. Such comments are badly
 * handled by default IntelliJ IDEA code formatter. We should put no effort in making and keeping ktlint in sync with
 * such bad code formatting.
 *
 * ```kotlin
 *     fun <T>
 *     // some comment
 *         foo(t: T) = "some-result"
 *
 *     fun <T>
 *         /* some comment
 *      *
 *      */
 *         foo(t: T) = "some-result"
 * ```
 */
public class DiscouragedCommentLocationRule :
    StandardRule("discouraged-comment-location"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isPartOfComment()) {
            // Be restrictive when adding new locations at which comments are discouraged. Always run against major
            // open source projects first to verify whether valid cases are found to comment at this location.
            if (node.afterNodeOfElementType(TYPE_PARAMETER_LIST) ||
                node.betweenNodesOfElementType(RPAR, THEN) ||
                node.betweenNodesOfElementType(THEN, ELSE_KEYWORD) ||
                node.betweenNodesOfElementType(ELSE_KEYWORD, ELSE) ||
                node.afterNodeOfElementType(DOT) ||
                node.afterNodeOfElementType(SAFE_ACCESS)
            ) {
                emit(node.startOffset, "No comment expected at this location", false)
            }
            when (node.treeParent.elementType) {
                VALUE_ARGUMENT, VALUE_PARAMETER, TYPE_PROJECTION, TYPE_PARAMETER ->
                    visitListElement(node, emit)
                VALUE_ARGUMENT_LIST, VALUE_PARAMETER_LIST, TYPE_ARGUMENT_LIST, TYPE_PARAMETER_LIST ->
                    visitList(node, emit)
            }
        }
    }

    private fun ASTNode.afterNodeOfElementType(afterElementType: IElementType) = prevCodeSibling()?.elementType == afterElementType

    private fun ASTNode.beforeNodeOfElementType(beforeElementType: IElementType) = nextCodeSibling()?.elementType == beforeElementType

    private fun ASTNode.betweenNodesOfElementType(
        afterElementType: IElementType,
        beforeElementType: IElementType,
    ) = afterNodeOfElementType(afterElementType) && beforeNodeOfElementType(beforeElementType)

    private fun visitListElement(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            EOL_COMMENT, BLOCK_COMMENT -> {
                // Disallow a comment inside a VALUE_PARAMETER. Note that an EOL comment which is placed on a separate line is a direct
                // child of the list.
                //      class Foo(
                //         val bar:
                //             // some comment
                //             Bar,
                //     )
                // or
                //      class Foo(
                //         val bar: Bar // some comment
                //     )
                // Although the last example looks ok it is not accepted because it can mess up a lot of things when trailing commas are
                // enabled and/or multiple arguments are used in the class.
                // It seems not to be possible to create a comment in a VALUE_ARGUMENT, TYPE_PROJECTION or TYPE_PARAMETER as a direct child
                // of that type. However, if it would be possible, it is ok to emit the error analog to the VALUE_ARGUMENT
                emit(
                    node.startOffset,
                    "A comment in a '${node.treeParentElementTypeName()}' is only allowed when placed on a new line before this element",
                    false,
                )
            }
            KDOC -> {
                if (node.treeParent.elementType == VALUE_PARAMETER) {
                    // Contrary to other comments, a kdoc which is placed on a separate line before the val keyword is part of the
                    // VALUE_PARAMETER ast node and is to be allowed.
                    if (node != node.treeParent.firstChildNode) {
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
                            "A kdoc in a '${node.treeParentElementTypeName()}' is only allowed when placed on a new line before this " +
                                "element",
                            false,
                        )
                    }
                } else {
                    emit(
                        node.startOffset,
                        "A KDoc is not allowed on a '${node.treeParentElementTypeName()}",
                        false,
                    )
                }
            }
        }
    }

    private fun ASTNode.treeParentElementTypeName() =
        treeParent
            .elementType
            .toString()
            .lowercase()

    private fun visitList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            EOL_COMMENT, BLOCK_COMMENT -> {
                if (!node.prevLeaf().isWhiteSpaceWithNewline()) {
                    // Disallow
                    //     class Foo(
                    //         val bar1: Bar, // some comment 1
                    //         // some comment 2
                    //         val bar2: Bar,
                    //     )
                    // It is not clear whether "some comment 2" belongs to bar1 as a continuation of "some comment 1" or that is belongs to
                    // bar2.
                    emit(
                        node.startOffset,
                        "A comment in a '${node.treeParentElementTypeName()}' is only allowed when placed on a separate line",
                        false,
                    )
                }
            }
            KDOC -> {
                emit(
                    node.startOffset,
                    "A KDoc is not allowed on a '${node.treeParentElementTypeName()}'",
                    false,
                )
            }
        }
    }
}

public val DISCOURAGED_COMMENT_LOCATION_RULE_ID: RuleId = DiscouragedCommentLocationRule().ruleId
