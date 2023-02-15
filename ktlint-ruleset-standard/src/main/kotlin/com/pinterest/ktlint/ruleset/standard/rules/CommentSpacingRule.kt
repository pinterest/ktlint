package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class CommentSpacingRule : StandardRule("comment-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == EOL_COMMENT) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf !is PsiWhiteSpace && prevLeaf is LeafPsiElement) {
                emit(node.startOffset, "Missing space before //", true)
                if (autoCorrect) {
                    node.upsertWhitespaceBeforeMe(" ")
                }
            }
            val text = node.getText()
            if (text.length != 2 &&
                !text.startsWith("// ") &&
                !text.startsWith("//noinspection") &&
                !text.startsWith("//region") &&
                !text.startsWith("//endregion") &&
                !text.startsWith("//language=")
            ) {
                emit(node.startOffset, "Missing space after //", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("// " + text.removePrefix("//"))
                }
            }
        }
    }
}

public val COMMENT_SPACING_RULE_ID: RuleId = CommentSpacingRule().ruleId
