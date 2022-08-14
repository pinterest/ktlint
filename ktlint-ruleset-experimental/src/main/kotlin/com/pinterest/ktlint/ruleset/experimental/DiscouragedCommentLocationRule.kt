package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.prevCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

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
public class DiscouragedCommentLocationRule : Rule("$experimentalRulesetId:discouraged-comment-location") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.isPartOfComment() }
            ?.prevCodeSibling()
            ?.let { codeSiblingBeforeComment ->
                // Be restrictive when adding new locations at which comments are discouraged. Always run against major
                // open source projects first to verify whether valid cases are found to comment at this location.
                if (codeSiblingBeforeComment.elementType == TYPE_PARAMETER_LIST) {
                    emit(
                        node.startOffset,
                        "No comment expected at this location",
                        false,
                    )
                }
            }
    }
}
