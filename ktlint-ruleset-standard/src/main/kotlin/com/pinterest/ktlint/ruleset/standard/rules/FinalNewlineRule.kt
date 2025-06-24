package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isRoot20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

@SinceKtlint("0.9", STABLE)
public class FinalNewlineRule :
    StandardRule(
        id = "final-newline",
        usesEditorConfigProperties = setOf(INSERT_FINAL_NEWLINE_PROPERTY),
    ) {
    private var insertFinalNewline = INSERT_FINAL_NEWLINE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        insertFinalNewline = editorConfig[INSERT_FINAL_NEWLINE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isRoot20) {
            if (node.textLength == 0) {
                stopTraversalOfAST()
                return
            }
            val lastNode = lastChildNodeOf(node)
            when {
                insertFinalNewline && (!lastNode.isWhiteSpace20 || lastNode.isWhiteSpaceWithoutNewline20) -> {
                    emit(node.textLength - 1, "File must end with a newline (\\n)", true)
                        .ifAutocorrectAllowed {
                            node.addChild(PsiWhiteSpaceImpl("\n"), null)
                        }
                }

                !insertFinalNewline && lastNode != null && lastNode.isWhiteSpaceWithNewline20 -> {
                    emit(lastNode.startOffset, "Redundant newline (\\n) at the end of file", true)
                        .ifAutocorrectAllowed { lastNode.remove() }
                }
            }
        }
        stopTraversalOfAST()
    }

    private tailrec fun lastChildNodeOf(node: ASTNode): ASTNode? =
        if (node.lastChildNode == null) node else lastChildNodeOf(node.lastChildNode)
}

public val FINAL_NEWLINE_RULE_ID: RuleId = FinalNewlineRule().ruleId
