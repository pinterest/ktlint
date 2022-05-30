package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

public const val experimentalRulesetId = "experimental"

public class ExperimentalRuleSetProvider : RuleSetProvider {
    override fun get(): RuleSet = RuleSet(
        experimentalRulesetId,
        UnnecessaryParenthesesBeforeTrailingLambdaRule(),
        TypeParameterListSpacingRule(),
        TypeArgumentListSpacingRule(),
        BlockCommentInitialStarAlignmentRule(),
        DiscouragedCommentLocationRule(),
        FunKeywordSpacingRule(),
        FunctionTypeReferenceSpacingRule(),
        ModifierListSpacingRule(),
        CommentWrappingRule(),
        KdocWrappingRule(),
        SpacingBetweenFunctionNameAndOpeningParenthesisRule(),
        ParameterListSpacingRule(),
        FunctionReturnTypeSpacingRule(),
        FunctionStartOfBodySpacingRule(),
        NullableTypeSpacingRule(),
        FunctionSignatureRule()
    )
}
