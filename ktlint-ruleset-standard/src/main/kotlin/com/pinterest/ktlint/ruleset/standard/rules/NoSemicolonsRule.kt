package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

@SinceKtlint("0.1", STABLE)
public class NoSemicolonsRule :
    StandardRule(
        id = "no-semi",
        visitorModifiers =
            setOf(
                RunAfterRule(
                    ruleId = WRAPPING_RULE_ID,
                    mode = RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                ),
            ),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != SEMICOLON) {
            return
        }
        val nextLeaf = node.nextLeaf()
        if (nextLeaf.doesNotRequirePreSemi() && isNoSemicolonRequiredAfter(node)) {
            emit(node.startOffset, "Unnecessary semicolon", true)
                .ifAutocorrectAllowed {
                    val prevLeaf = node.prevLeaf(true)
                    node.treeParent.removeChild(node)
                    if (prevLeaf.isWhiteSpace() && (nextLeaf == null || nextLeaf.isWhiteSpace())) {
                        node.treeParent.removeChild(prevLeaf!!)
                    }
                }
        } else if (nextLeaf !is PsiWhiteSpace) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) {
                return
            }
            // todo: move to a separate rule
            emit(node.startOffset + 1, "Missing spacing after \";\"", true)
                .ifAutocorrectAllowed {
                    node.upsertWhitespaceAfterMe(" ")
                }
        }
    }

    private fun ASTNode?.doesNotRequirePreSemi() =
        when {
            this == null -> true

            this is PsiWhiteSpace -> {
                nextLeaf {
                    val psi = it.psi
                    it !is PsiWhiteSpace &&
                        it !is PsiComment &&
                        psi.getStrictParentOfType<KDoc>() == null &&
                        psi.getStrictParentOfType<KtAnnotationEntry>() == null
                }.let { nextLeaf ->
                    nextLeaf == null ||
                        // \s+ and then eof
                        (textContains('\n') && nextLeaf.elementType != KtTokens.LBRACE)
                }
            }

            else -> false
        }

    private fun isNoSemicolonRequiredAfter(node: ASTNode): Boolean {
        val prevCodeLeaf =
            node.prevCodeLeaf()
                ?: return true
        if (prevCodeLeaf.elementType == OBJECT_KEYWORD) {
            // https://github.com/pinterest/ktlint/issues/281
            return false
        }

        val parent = prevCodeLeaf.treeParent?.psi
        if (parent is KtLoopExpression && parent !is KtDoWhileExpression && parent.body == null) {
            // https://github.com/pinterest/ktlint/issues/955
            return false
        }
        if (parent is KtIfExpression && parent.then == null) {
            return false
        }
        // In case of an enum entry the semicolon (e.g. the node) is a direct child node of enum entry
        if (node.treeParent.elementType == ENUM_ENTRY) {
            return node.isLastCodeLeafBeforeClosingOfClassBody()
        }

        return true
    }

    private fun ASTNode?.isLastCodeLeafBeforeClosingOfClassBody() = getLastCodeLeafBeforeClosingOfClassBody() == this

    private fun ASTNode?.getLastCodeLeafBeforeClosingOfClassBody() =
        this
            ?.parent(CLASS_BODY)
            ?.lastChildLeafOrSelf()
            ?.prevCodeLeaf()
}

public val NO_SEMICOLONS_RULE_ID: RuleId = NoSemicolonsRule().ruleId
