package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule

public const val experimentalRulesetId = "experimental"

public class ExperimentalRuleSetProvider : RuleSetProvider {
    override fun get(): RuleSet = RuleSet(
        experimentalRulesetId,
        AnnotationRule(),
        ArgumentListWrappingRule(),
        MultiLineIfElseRule(),
        NoEmptyFirstLineInMethodBlockRule(),
        TrailingCommaRule(),
        PackageNameRule(),
        EnumEntryNameCaseRule(),
        SpacingAroundDoubleColonRule(),
        SpacingBetweenDeclarationsWithCommentsRule(),
        SpacingBetweenDeclarationsWithAnnotationsRule(),
        SpacingAroundAngleBracketsRule(),
        SpacingAroundUnaryOperatorRule(),
        AnnotationSpacingRule(),
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
