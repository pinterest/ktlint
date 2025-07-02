package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CATCH_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FINALLY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_NAME
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet.create

@SinceKtlint("0.1", STABLE)
public class SpacingAroundKeywordRule : StandardRule("keyword-spacing") {
    private val noLFBeforeSet = create(ELSE_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD)
    private val tokenSet =
        create(
            CATCH_KEYWORD,
            DO_KEYWORD,
            ELSE_KEYWORD,
            FINALLY_KEYWORD,
            FOR_KEYWORD,
            IF_KEYWORD,
            TRY_KEYWORD,
            WHEN_KEYWORD,
            WHILE_KEYWORD,
        )

    private val keywordsWithoutSpaces = create(GET_KEYWORD, SET_KEYWORD)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType in tokenSet && node.parent?.elementType != KDOC_NAME && !node.nextLeaf.isWhiteSpace20) {
            emit(node.startOffset + node.text.length, "Missing spacing after \"${node.text}\"", true)
                .ifAutocorrectAllowed { node.upsertWhitespaceAfterMe(" ") }
        }
        node
            .takeIf { keywordsWithoutSpaces.contains(it.elementType) }
            .takeIf { node.isPropertyAccessorWithValueParameterList() }
            ?.nextLeaf
            ?.takeIf { it.isWhiteSpace20 }
            ?.also { nextLeaf ->
                emit(node.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                    .ifAutocorrectAllowed { nextLeaf.remove() }
            }
        if (noLFBeforeSet.contains(node.elementType)) {
            val prevLeaf = node.prevLeaf
            val isElseKeyword = node.elementType == ELSE_KEYWORD
            if (prevLeaf.isWhiteSpaceWithNewline20 &&
                (!isElseKeyword || node.parent?.elementType != WHEN_ENTRY)
            ) {
                val rBrace = prevLeaf?.prevLeaf?.takeIf { it.elementType == RBRACE }
                rBrace
                    ?.parent
                    ?.takeIf { it.elementType == BLOCK }
                    ?.takeIf { !isElseKeyword || it.parent?.parent == node.parent }
                    ?.run {
                        emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                            .ifAutocorrectAllowed { prevLeaf.replaceTextWith(" ") }
                    }
            }
        }
    }

    private fun ASTNode.isPropertyAccessorWithValueParameterList() =
        null !=
            parent
                ?.takeIf { it.elementType == PROPERTY_ACCESSOR }
                ?.findChildByType(VALUE_PARAMETER_LIST)
}

public val SPACING_AROUND_KEYWORD_RULE_ID: RuleId = SpacingAroundKeywordRule().ruleId
