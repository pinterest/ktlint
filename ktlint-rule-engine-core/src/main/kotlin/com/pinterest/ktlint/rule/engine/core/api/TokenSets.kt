package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARRAY_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALLABLE_REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXPRESSION_CODE_FRAGMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INDICES
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LABEL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LABELED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LABEL_QUALIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THIS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_CODE_FRAGMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE_KEYWORD
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

public object TokenSets {
    public val COMMENTS: TokenSet = TokenSet.create(BLOCK_COMMENT, EOL_COMMENT, KDOC)
    public val EXPRESSIONS: TokenSet =
        TokenSet.create(
            ANNOTATED_EXPRESSION,
            ARRAY_ACCESS_EXPRESSION,
            BINARY_EXPRESSION,
            BINARY_WITH_TYPE,
            CALLABLE_REFERENCE_EXPRESSION,
            CALL_EXPRESSION,
            CLASS_LITERAL_EXPRESSION,
            COLLECTION_LITERAL_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,
            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION,
            EXPRESSION_CODE_FRAGMENT,
            FUNCTION_LITERAL,
            INDICES,
            IS_EXPRESSION,
            LABEL,
            LABELED_EXPRESSION,
            LABEL_QUALIFIER,
            LAMBDA_EXPRESSION,
            OBJECT_LITERAL,
            OPERATION_REFERENCE,
            POSTFIX_EXPRESSION,
            PREFIX_EXPRESSION,
            REFERENCE_EXPRESSION,
            SAFE_ACCESS_EXPRESSION,
            SUPER_EXPRESSION,
            THIS_EXPRESSION,
            TYPE_CODE_FRAGMENT,
            WHEN,
        )
    public val CONTROL_FLOW_KEYWORDS: TokenSet =
        TokenSet.create(
            DO_KEYWORD,
            IF_KEYWORD,
            WHILE_KEYWORD,
        )
}
