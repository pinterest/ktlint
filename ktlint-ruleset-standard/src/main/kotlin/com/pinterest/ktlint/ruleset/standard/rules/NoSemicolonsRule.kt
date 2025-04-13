package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens

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
                    node.remove()
                    if (prevLeaf.isWhiteSpace() && (nextLeaf == null || nextLeaf.isWhiteSpace())) {
                        prevLeaf.remove()
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
            this == null -> {
                true
            }

            this is PsiWhiteSpace -> {
                nextLeaf {
                    it !is PsiWhiteSpace &&
                        it !is PsiComment &&
                        it.parent(ElementType.KDOC) == null &&
                        it.parent(ElementType.ANNOTATION_ENTRY) == null
                }.let { nextLeaf ->
                    nextLeaf == null ||
                        // \s+ and then eof
                        (textContains('\n') && nextLeaf.elementType != KtTokens.LBRACE)
                }
            }

            else -> {
                false
            }
        }

    private fun isNoSemicolonRequiredAfter(node: ASTNode): Boolean {
        val prevCodeLeaf =
            node.prevCodeLeaf()
                ?: return true
        if (prevCodeLeaf.elementType == OBJECT_KEYWORD) {
            // https://github.com/pinterest/ktlint/issues/281
            return false
        }

        val parentNode = prevCodeLeaf.treeParent
        if (parentNode.isLoopWithoutBody()) {
            // https://github.com/pinterest/ktlint/issues/955
            return false
        }
        if (parentNode.isIfExpressionWithoutThen()) {
            return false
        }
        // In case of an enum entry the semicolon (e.g. the node) is a direct child node of enum entry
        if (node.treeParent.elementType == ENUM_ENTRY) {
            return node.isLastCodeLeafBeforeClosingOfClassBody()
        }
        if (node.isEnumClassWithoutValues()) {
            return false
        }

        return true
    }

    private fun ASTNode.isLoopWithoutBody() =
        (elementType == WHILE || elementType == ElementType.FOR) &&
            findChildByType(ElementType.BODY)?.firstChildNode == null

    private fun ASTNode.isIfExpressionWithoutThen() = elementType == ElementType.IF && findChildByType(THEN)?.firstChildNode == null

    private fun ASTNode?.isLastCodeLeafBeforeClosingOfClassBody() = getLastCodeLeafBeforeClosingOfClassBody() == this

    private fun ASTNode?.getLastCodeLeafBeforeClosingOfClassBody() =
        this
            ?.parent(CLASS_BODY)
            ?.lastChildLeafOrSelf()
            ?.prevCodeLeaf()

    private fun ASTNode?.isEnumClassWithoutValues() =
        this
            ?.takeIf { !it.isLastCodeLeafBeforeClosingOfClassBody() }
            ?.parent(CLASS_BODY)
            ?.takeIf { this == it.firstChildNode.nextCodeSibling() }
            ?.parent(CLASS)
            ?.hasModifier(ENUM_KEYWORD)
            ?: false
}

public val NO_SEMICOLONS_RULE_ID: RuleId = NoSemicolonsRule().ruleId
