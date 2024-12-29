package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE_KEYWORD
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens

public object TokenSets {
    public val COMMENTS: TokenSet = KtTokens.COMMENTS

    /**
     *
     * Reference: This is a subset of [KotlinExpressionParsing.EXPRESSION_FIRST]
     */
    public val CONTROL_FLOW_KEYWORDS: TokenSet =
        TokenSet.create(
            IF_KEYWORD, // if
            WHEN_KEYWORD, // when
            TRY_KEYWORD, // try
            OBJECT_KEYWORD, // object
            // loop
            FOR_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,
        )
}
