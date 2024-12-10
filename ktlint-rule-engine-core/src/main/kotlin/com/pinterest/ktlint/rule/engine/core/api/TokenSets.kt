package com.pinterest.ktlint.rule.engine.core.api

import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

public object TokenSets {
    public val COMMENTS: TokenSet = TokenSet.create(ElementType.BLOCK_COMMENT, ElementType.EOL_COMMENT, ElementType.KDOC)
    public val EXPRESSIONS: TokenSet =
        TokenSet.create(
            ElementType.LAMBDA_EXPRESSION,
            ElementType.FUNCTION_LITERAL,
            ElementType.ANNOTATED_EXPRESSION,
            ElementType.REFERENCE_EXPRESSION,
            ElementType.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION,
            ElementType.OPERATION_REFERENCE,
            ElementType.LABEL,
            ElementType.LABEL_QUALIFIER,
            ElementType.THIS_EXPRESSION,
            ElementType.SUPER_EXPRESSION,
            ElementType.BINARY_EXPRESSION,
            ElementType.BINARY_WITH_TYPE,
            ElementType.IS_EXPRESSION,
            ElementType.PREFIX_EXPRESSION,
            ElementType.POSTFIX_EXPRESSION,
            ElementType.LABELED_EXPRESSION,
            ElementType.CALL_EXPRESSION,
            ElementType.ARRAY_ACCESS_EXPRESSION,
            ElementType.INDICES,
            ElementType.DOT_QUALIFIED_EXPRESSION,
            ElementType.CALLABLE_REFERENCE_EXPRESSION,
            ElementType.CLASS_LITERAL_EXPRESSION,
            ElementType.SAFE_ACCESS_EXPRESSION,
            ElementType.OBJECT_LITERAL,
            ElementType.WHEN,
            ElementType.COLLECTION_LITERAL_EXPRESSION,
            ElementType.TYPE_CODE_FRAGMENT,
            ElementType.EXPRESSION_CODE_FRAGMENT,
        )
}
