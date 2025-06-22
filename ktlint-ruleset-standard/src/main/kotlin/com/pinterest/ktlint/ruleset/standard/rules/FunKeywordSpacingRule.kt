package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Lints and formats the spacing after the fun keyword
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class FunKeywordSpacingRule : StandardRule("fun-keyword-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == FUN_KEYWORD }
            ?.nextLeaf
            ?.let { leafAfterFunKeyword ->
                when {
                    leafAfterFunKeyword.elementType == ElementType.WHITE_SPACE && leafAfterFunKeyword.text != " " -> {
                        emit(
                            leafAfterFunKeyword.startOffset,
                            "Single space expected after the fun keyword",
                            true,
                        ).ifAutocorrectAllowed {
                            (leafAfterFunKeyword as LeafPsiElement).rawReplaceWithText(" ")
                        }
                    }

                    leafAfterFunKeyword.elementType == ElementType.IDENTIFIER -> {
                        // Identifier can only be adjacent to fun keyword in case the identifier is wrapped between backticks:
                        //     fun`foo`() {}
                        emit(leafAfterFunKeyword.startOffset, "Space expected between the fun keyword and backtick", true)
                            .ifAutocorrectAllowed {
                                leafAfterFunKeyword.treeParent.addChild(PsiWhiteSpaceImpl(" "), leafAfterFunKeyword)
                            }
                    }

                    else -> {
                        Unit
                    }
                }
            }
    }
}

public val FUN_KEYWORD_SPACING_RULE: RuleId = FunKeywordSpacingRule().ruleId
