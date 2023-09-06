package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_MARKDOWN_LINK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_TAG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

@SinceKtlint("0.2", STABLE)
public class NoMultipleSpacesRule : StandardRule("no-multi-spaces") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node is PsiWhiteSpace }
            .takeUnless { node.isPossibleAlignmentOfKdocTag() }
            ?.let {
                val beforeIndentation = node.removeIndentation()
                if (beforeIndentation.length > 1) {
                    emit(node.startOffset + 1, "Unnecessary long whitespace", true)
                    if (autoCorrect) {
                        val remainder = node.text.substring(beforeIndentation.length)
                        (node as LeafPsiElement).rawReplaceWithText(" $remainder")
                    }
                }
            }
    }

    private fun ASTNode.removeIndentation() = this.text.substringBefore("\n")

    // allow multiple spaces in KDoc in case of KDOC_TAG for alignment, e.g.
    // @param foo      stuff
    // @param foobar   stuff2
    private fun ASTNode.isPossibleAlignmentOfKdocTag() = treePrev?.elementType == KDOC_MARKDOWN_LINK && treeParent?.elementType == KDOC_TAG
}

public val NO_MULTIPLE_SPACES_RULE_ID: RuleId = NoMultipleSpacesRule().ruleId
