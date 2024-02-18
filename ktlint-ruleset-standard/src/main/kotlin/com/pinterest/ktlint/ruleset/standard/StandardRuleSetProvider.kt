package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.ruleset.standard.rules.AnnotationRule
import com.pinterest.ktlint.ruleset.standard.rules.AnnotationSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.ArgumentListWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.BackingPropertyNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.BinaryExpressionWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.BlankLineBeforeDeclarationRule
import com.pinterest.ktlint.ruleset.standard.rules.BlockCommentInitialStarAlignmentRule
import com.pinterest.ktlint.ruleset.standard.rules.ChainMethodContinuationRule
import com.pinterest.ktlint.ruleset.standard.rules.ChainWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ClassNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.ClassSignatureRule
import com.pinterest.ktlint.ruleset.standard.rules.CommentSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.CommentWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ConditionWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ContextReceiverWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.DiscouragedCommentLocationRule
import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule
import com.pinterest.ktlint.ruleset.standard.rules.EnumWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.FilenameRule
import com.pinterest.ktlint.ruleset.standard.rules.FinalNewlineRule
import com.pinterest.ktlint.ruleset.standard.rules.FunKeywordSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionExpressionBodyRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionLiteralRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionReturnTypeSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionStartOfBodySpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionTypeModifierSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionTypeReferenceSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.IfElseBracingRule
import com.pinterest.ktlint.ruleset.standard.rules.IfElseWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ImportOrderingRule
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule
import com.pinterest.ktlint.ruleset.standard.rules.KdocRule
import com.pinterest.ktlint.ruleset.standard.rules.KdocWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.MaxLineLengthRule
import com.pinterest.ktlint.ruleset.standard.rules.MixedConditionOperatorsRule
import com.pinterest.ktlint.ruleset.standard.rules.ModifierListSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.ModifierOrderRule
import com.pinterest.ktlint.ruleset.standard.rules.MultiLineIfElseRule
import com.pinterest.ktlint.ruleset.standard.rules.MultilineExpressionWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.MultilineLoopRule
import com.pinterest.ktlint.ruleset.standard.rules.NoBlankLineBeforeRbraceRule
import com.pinterest.ktlint.ruleset.standard.rules.NoBlankLineInListRule
import com.pinterest.ktlint.ruleset.standard.rules.NoBlankLinesInChainedMethodCallsRule
import com.pinterest.ktlint.ruleset.standard.rules.NoConsecutiveBlankLinesRule
import com.pinterest.ktlint.ruleset.standard.rules.NoConsecutiveCommentsRule
import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyClassBodyRule
import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyFileRule
import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyFirstLineInClassBodyRule
import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyFirstLineInMethodBlockRule
import com.pinterest.ktlint.ruleset.standard.rules.NoLineBreakAfterElseRule
import com.pinterest.ktlint.ruleset.standard.rules.NoLineBreakBeforeAssignmentRule
import com.pinterest.ktlint.ruleset.standard.rules.NoMultipleSpacesRule
import com.pinterest.ktlint.ruleset.standard.rules.NoSemicolonsRule
import com.pinterest.ktlint.ruleset.standard.rules.NoSingleLineBlockCommentRule
import com.pinterest.ktlint.ruleset.standard.rules.NoTrailingSpacesRule
import com.pinterest.ktlint.ruleset.standard.rules.NoUnitReturnRule
import com.pinterest.ktlint.ruleset.standard.rules.NoUnusedImportsRule
import com.pinterest.ktlint.ruleset.standard.rules.NoWildcardImportsRule
import com.pinterest.ktlint.ruleset.standard.rules.NullableTypeSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.PackageNameRule
import com.pinterest.ktlint.ruleset.standard.rules.ParameterListSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.ParameterListWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ParameterWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.PropertyNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.PropertyWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundAngleBracketsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundColonRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundCommaRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundCurlyRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundDotRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundDoubleColonRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundKeywordRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundOperatorsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundParensRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundRangeOperatorRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundUnaryOperatorRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenDeclarationsWithAnnotationsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenDeclarationsWithCommentsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenFunctionNameAndOpeningParenthesisRule
import com.pinterest.ktlint.ruleset.standard.rules.StatementWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.StringTemplateIndentRule
import com.pinterest.ktlint.ruleset.standard.rules.StringTemplateRule
import com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnCallSiteRule
import com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnDeclarationSiteRule
import com.pinterest.ktlint.ruleset.standard.rules.TryCatchFinallySpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.TypeArgumentCommentRule
import com.pinterest.ktlint.ruleset.standard.rules.TypeArgumentListSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.TypeParameterCommentRule
import com.pinterest.ktlint.ruleset.standard.rules.TypeParameterListSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.UnnecessaryParenthesesBeforeTrailingLambdaRule
import com.pinterest.ktlint.ruleset.standard.rules.ValueArgumentCommentRule
import com.pinterest.ktlint.ruleset.standard.rules.ValueParameterCommentRule
import com.pinterest.ktlint.ruleset.standard.rules.WrappingRule

public class StandardRuleSetProvider : RuleSetProviderV3(RuleSetId.STANDARD) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { AnnotationRule() },
            RuleProvider { AnnotationSpacingRule() },
            RuleProvider { ArgumentListWrappingRule() },
            RuleProvider { BackingPropertyNamingRule() },
            RuleProvider { BinaryExpressionWrappingRule() },
            RuleProvider { BlankLineBeforeDeclarationRule() },
            RuleProvider { BlockCommentInitialStarAlignmentRule() },
            RuleProvider { ChainMethodContinuationRule() },
            RuleProvider { ChainWrappingRule() },
            RuleProvider { ClassNamingRule() },
            RuleProvider { ClassSignatureRule() },
            RuleProvider { ConditionWrappingRule() },
            RuleProvider { CommentSpacingRule() },
            RuleProvider { CommentWrappingRule() },
            RuleProvider { ContextReceiverWrappingRule() },
            RuleProvider { DiscouragedCommentLocationRule() },
            RuleProvider { EnumEntryNameCaseRule() },
            RuleProvider { EnumWrappingRule() },
            RuleProvider { FilenameRule() },
            RuleProvider { FinalNewlineRule() },
            RuleProvider { FunctionExpressionBodyRule() },
            RuleProvider { FunctionLiteralRule() },
            RuleProvider { FunctionNamingRule() },
            RuleProvider { FunctionReturnTypeSpacingRule() },
            RuleProvider { FunctionSignatureRule() },
            RuleProvider { FunctionStartOfBodySpacingRule() },
            RuleProvider { FunctionTypeModifierSpacingRule() },
            RuleProvider { FunctionTypeReferenceSpacingRule() },
            RuleProvider { FunKeywordSpacingRule() },
            RuleProvider { IfElseBracingRule() },
            RuleProvider { IfElseWrappingRule() },
            RuleProvider { ImportOrderingRule() },
            RuleProvider { IndentationRule() },
            RuleProvider { KdocRule() },
            RuleProvider { KdocWrappingRule() },
            RuleProvider { MaxLineLengthRule() },
            RuleProvider { MixedConditionOperatorsRule() },
            RuleProvider { ModifierListSpacingRule() },
            RuleProvider { ModifierOrderRule() },
            RuleProvider { MultiLineIfElseRule() },
            RuleProvider { MultilineExpressionWrappingRule() },
            RuleProvider { MultilineLoopRule() },
            RuleProvider { NoBlankLineBeforeRbraceRule() },
            RuleProvider { NoBlankLineInListRule() },
            RuleProvider { NoBlankLinesInChainedMethodCallsRule() },
            RuleProvider { NoConsecutiveBlankLinesRule() },
            RuleProvider { NoConsecutiveCommentsRule() },
            RuleProvider { NoEmptyClassBodyRule() },
            RuleProvider { NoEmptyFileRule() },
            RuleProvider { NoEmptyFirstLineInClassBodyRule() },
            RuleProvider { NoEmptyFirstLineInMethodBlockRule() },
            RuleProvider { NoLineBreakAfterElseRule() },
            RuleProvider { NoLineBreakBeforeAssignmentRule() },
            RuleProvider { NoMultipleSpacesRule() },
            RuleProvider { NoSemicolonsRule() },
            RuleProvider { NoSingleLineBlockCommentRule() },
            RuleProvider { NoTrailingSpacesRule() },
            RuleProvider { NoUnitReturnRule() },
            RuleProvider { NoUnusedImportsRule() },
            RuleProvider { NoWildcardImportsRule() },
            RuleProvider { NullableTypeSpacingRule() },
            RuleProvider { PackageNameRule() },
            RuleProvider { ParameterListSpacingRule() },
            RuleProvider { ParameterListWrappingRule() },
            RuleProvider { ParameterWrappingRule() },
            RuleProvider { PropertyNamingRule() },
            RuleProvider { PropertyWrappingRule() },
            RuleProvider { SpacingAroundAngleBracketsRule() },
            RuleProvider { SpacingAroundColonRule() },
            RuleProvider { SpacingAroundCommaRule() },
            RuleProvider { SpacingAroundCurlyRule() },
            RuleProvider { SpacingAroundDotRule() },
            RuleProvider { SpacingAroundDoubleColonRule() },
            RuleProvider { SpacingAroundKeywordRule() },
            RuleProvider { SpacingAroundOperatorsRule() },
            RuleProvider { SpacingAroundParensRule() },
            RuleProvider { SpacingAroundRangeOperatorRule() },
            RuleProvider { SpacingAroundUnaryOperatorRule() },
            RuleProvider { SpacingBetweenDeclarationsWithAnnotationsRule() },
            RuleProvider { SpacingBetweenDeclarationsWithCommentsRule() },
            RuleProvider { SpacingBetweenFunctionNameAndOpeningParenthesisRule() },
            RuleProvider { StatementWrappingRule() },
            RuleProvider { StringTemplateRule() },
            RuleProvider { StringTemplateIndentRule() },
            RuleProvider { TrailingCommaOnCallSiteRule() },
            RuleProvider { TrailingCommaOnDeclarationSiteRule() },
            RuleProvider { TryCatchFinallySpacingRule() },
            RuleProvider { TypeArgumentCommentRule() },
            RuleProvider { TypeArgumentListSpacingRule() },
            RuleProvider { TypeParameterCommentRule() },
            RuleProvider { TypeParameterListSpacingRule() },
            RuleProvider { ValueArgumentCommentRule() },
            RuleProvider { ValueParameterCommentRule() },
            RuleProvider { UnnecessaryParenthesesBeforeTrailingLambdaRule() },
            RuleProvider { WrappingRule() },
        )
}
