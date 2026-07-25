package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isRoot
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.ruleset.standard.StandardRule
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
        if (node.isRoot) {
            if (node.textLength == 0) {
                stopTraversalOfAST()
                return
            }
            val lastNode = lastChildNodeOf(node)
            when {
                insertFinalNewline && (!lastNode.isWhiteSpace || lastNode.isWhiteSpaceWithoutNewline) -> {
                    emit(node.textLength - 1, "File must end with a newline (\\n)", true)
                        .ifAutocorrectAllowed {
                            node.addChild(PsiWhiteSpaceImpl("\n"), null)
                        }
                }

                !insertFinalNewline && lastNode != null && lastNode.isWhiteSpaceWithNewline -> {
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
