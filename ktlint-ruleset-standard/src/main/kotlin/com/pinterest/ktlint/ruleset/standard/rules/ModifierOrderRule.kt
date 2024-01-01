package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ABSTRACT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ACTUAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMPANION_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONST_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DATA_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXPECT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXTERNAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FINAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INFIX_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INLINE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INNER_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INTERNAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LATEINIT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPEN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIVATE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROTECTED_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PUBLIC_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEALED_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUSPEND_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TAILREC_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VARARG_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import java.util.Arrays

@SinceKtlint("0.7", STABLE)
public class ModifierOrderRule : StandardRule("modifier-order") {
    // subset of ElementType.MODIFIER_KEYWORDS_ARRAY (+ annotations entries)
    private val order =
        arrayOf(
            ABSTRACT_KEYWORD,
            ACTUAL_KEYWORD,
            ANNOTATION_ENTRY,
            ANNOTATION_KEYWORD,
            COMPANION_KEYWORD,
            CONST_KEYWORD,
            DATA_KEYWORD,
            ENUM_KEYWORD,
            EXPECT_KEYWORD,
            EXTERNAL_KEYWORD,
            FINAL_KEYWORD,
            INFIX_KEYWORD,
            INLINE_KEYWORD,
            INNER_KEYWORD,
            INTERNAL_KEYWORD,
            LATEINIT_KEYWORD,
            OPEN_KEYWORD,
            OPERATOR_KEYWORD,
            OVERRIDE_KEYWORD,
            PRIVATE_KEYWORD,
            PROTECTED_KEYWORD,
            PUBLIC_KEYWORD,
            SEALED_KEYWORD,
            SUSPEND_KEYWORD,
            TAILREC_KEYWORD,
            VARARG_KEYWORD,
        )
    private val tokenSet = TokenSet.create(*order)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.psi is KtDeclarationModifierList) {
            val modifierArr = node.getChildren(tokenSet)
            val sorted = modifierArr.copyOf().apply { sortWith(compareBy { order.indexOf(it.elementType) }) }
            if (!Arrays.equals(modifierArr, sorted)) {
                // Since annotations can be fairly lengthy and/or span multiple lines we are
                // squashing them into a single placeholder text to guarantee a single line output
                squashAnnotations(sorted)
                    .joinToString(" ")
                    .let { squashedAnnotations ->
                        emit(
                            node.startOffset,
                            "Incorrect modifier order (should be \"$squashedAnnotations\")",
                            true,
                        )
                    }
                if (autoCorrect) {
                    modifierArr.forEachIndexed { i, n ->
                        node.replaceChild(n, sorted[i].clone() as ASTNode)
                    }
                }
            }
        }
    }

    private fun squashAnnotations(sorted: Array<ASTNode>): List<String> {
        val nonAnnotationModifiers = sorted.filter { it.psi !is KtAnnotationEntry }
        return if (nonAnnotationModifiers.size != sorted.size) {
            listOf("@Annotation...") + nonAnnotationModifiers.map { it.text }
        } else {
            nonAnnotationModifiers.map { it.text }
        }
    }
}

public val MODIFIER_ORDER_RULE_ID: RuleId = ModifierOrderRule().ruleId
