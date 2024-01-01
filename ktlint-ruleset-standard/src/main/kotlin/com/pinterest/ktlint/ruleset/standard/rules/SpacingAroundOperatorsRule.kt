package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANDAND
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIV
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIVEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQEQEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCLEQEQEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUSEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MUL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MULTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERCEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUSEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPrefixExpression

@SinceKtlint("0.1", STABLE)
public class SpacingAroundOperatorsRule : StandardRule("op-spacing") {
    private val tokenSet =
        TokenSet.create(
            ANDAND,
            ARROW,
            DIV,
            DIVEQ,
            ELVIS,
            EQ,
            EQEQ,
            EQEQEQ,
            EXCLEQ,
            EXCLEQEQEQ,
            GT,
            GTEQ,
            LT,
            LTEQ,
            MINUS,
            MINUSEQ,
            MUL,
            MULTEQ,
            OROR,
            PERC,
            PERCEQ,
            PLUS,
            PLUSEQ,
        )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (tokenSet.contains(node.elementType) &&
            node.isNotUnaryOperator() &&
            isNotSpreadOperator(node) &&
            isNotImport(node)
        ) {
            if ((node.elementType == LT || node.elementType == GT || node.elementType == MUL) &&
                node.treeParent.elementType != OPERATION_REFERENCE
            ) {
                // Do not format parameter types like:
                //   <T> fun foo(...)
                //   class Foo<T> { ... }
                //   Foo<*>
                return
            }
            val spacingBefore = node.prevLeaf() is PsiWhiteSpace
            val spacingAfter = node.nextLeaf() is PsiWhiteSpace
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                    }
                }
                !spacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
            }
        }
    }

    private fun ASTNode.isNotUnaryOperator() = !isPartOf(KtPrefixExpression::class)

    private fun isNotSpreadOperator(node: ASTNode) =
        // fn(*array)
        !(node.elementType == MUL && node.treeParent.elementType == VALUE_ARGUMENT)

    private fun isNotImport(node: ASTNode) =
        // import *
        !node.isPartOf(KtImportDirective::class)
}

public val SPACING_AROUND_OPERATORS_RULE_ID: RuleId = SpacingAroundOperatorsRule().ruleId
