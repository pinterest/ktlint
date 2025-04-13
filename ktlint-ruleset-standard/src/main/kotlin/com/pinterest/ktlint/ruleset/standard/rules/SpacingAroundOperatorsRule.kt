package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
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
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
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
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

@SinceKtlint("0.1", STABLE)
public class SpacingAroundOperatorsRule : StandardRule("op-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isUnaryOperator()) {
            // Allow:
            //   val foo = -1
            return
        }

        if (node.isSpreadOperator()) {
            // Allow:
            //   foo(*array)
            return
        }

        if (node.isImport()) {
            // Allow:
            //   import *
            return
        }

        if ((node.elementType == LT || node.elementType == GT || node.elementType == MUL) &&
            node.treeParent.elementType != OPERATION_REFERENCE
        ) {
            // Allow:
            //   <T> fun foo(...)
            //   class Foo<T> { ... }
            //   Foo<*>
            return
        }

        if (node.elementType in OPERATORS ||
            (node.elementType == IDENTIFIER && node.treeParent.elementType == OPERATION_REFERENCE)
        ) {
            val spacingBefore = node.prevLeaf() is PsiWhiteSpace
            val spacingAfter = node.nextLeaf() is PsiWhiteSpace
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceBeforeMe(" ")
                            node.upsertWhitespaceAfterMe(" ")
                        }
                }

                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceBeforeMe(" ")
                        }
                }

                !spacingAfter -> {
                    emit(node.startOffset + node.textLength, "Missing spacing after \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            node.upsertWhitespaceAfterMe(" ")
                        }
                }
            }
        }
    }

    private fun ASTNode.isUnaryOperator() =
        PREFIX_EXPRESSION ==
            parent { it.elementType == OPERATION_REFERENCE }
                ?.treeParent
                ?.elementType

    private fun ASTNode.isSpreadOperator() =
        // fn(*array)
        elementType == MUL && treeParent.elementType == VALUE_ARGUMENT

    private fun ASTNode.isImport() =
        // import *
        isPartOf(ElementType.IMPORT_DIRECTIVE)

    private companion object {
        private val OPERATORS =
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
    }
}

public val SPACING_AROUND_OPERATORS_RULE_ID: RuleId = SpacingAroundOperatorsRule().ruleId
