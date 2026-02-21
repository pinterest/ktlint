package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2Provider
import com.pinterest.ktlint.ruleset.standard.rules.AnnotationRule
import com.pinterest.ktlint.ruleset.standard.rules.AnnotationSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.ArgumentListWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.BackingPropertyNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.BinaryExpressionWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.BlankLineBeforeDeclarationRule
import com.pinterest.ktlint.ruleset.standard.rules.BlankLineBetweenWhenConditions
import com.pinterest.ktlint.ruleset.standard.rules.BlockCommentInitialStarAlignmentRule
import com.pinterest.ktlint.ruleset.standard.rules.ChainMethodContinuationRule
import com.pinterest.ktlint.ruleset.standard.rules.ChainWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ClassNamingRule
import com.pinterest.ktlint.ruleset.standard.rules.ClassSignatureRule
import com.pinterest.ktlint.ruleset.standard.rules.CommentSpacingRule
import com.pinterest.ktlint.ruleset.standard.rules.CommentWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ContextReceiverListWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ContextReceiverWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule
import com.pinterest.ktlint.ruleset.standard.rules.EnumWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.ExpressionOperandWrappingRule
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
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundSquareBracketsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundUnaryOperatorRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenDeclarationsWithAnnotationsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenDeclarationsWithCommentsRule
import com.pinterest.ktlint.ruleset.standard.rules.SpacingBetweenFunctionNameAndOpeningParenthesisRule
import com.pinterest.ktlint.ruleset.standard.rules.StatementWrappingRule
import com.pinterest.ktlint.ruleset.standard.rules.StringTemplateIndentRule
import com.pinterest.ktlint.ruleset.standard.rules.StringTemplateRule
import com.pinterest.ktlint.ruleset.standard.rules.ThenSpacingRule
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
import com.pinterest.ktlint.ruleset.standard.rules.WhenEntryBracing
import com.pinterest.ktlint.ruleset.standard.rules.WrappingRule

public class StandardRuleSetProvider : RuleSetV2Provider(RuleSetId.STANDARD) {
    override fun getRuleProviders(): Set<RuleV2Provider> =
        setOf(
            RuleV2Provider { AnnotationRule() },
            RuleV2Provider { AnnotationSpacingRule() },
            RuleV2Provider { ArgumentListWrappingRule() },
            RuleV2Provider { BackingPropertyNamingRule() },
            RuleV2Provider { BinaryExpressionWrappingRule() },
            RuleV2Provider { BlankLineBeforeDeclarationRule() },
            RuleV2Provider { BlankLineBetweenWhenConditions() },
            RuleV2Provider { BlockCommentInitialStarAlignmentRule() },
            RuleV2Provider { ChainMethodContinuationRule() },
            RuleV2Provider { ChainWrappingRule() },
            RuleV2Provider { ClassNamingRule() },
            RuleV2Provider { ClassSignatureRule() },
            RuleV2Provider { CommentSpacingRule() },
            RuleV2Provider { CommentWrappingRule() },
            RuleV2Provider { ContextReceiverWrappingRule() },
            RuleV2Provider { ContextReceiverListWrappingRule() },
            RuleV2Provider { EnumEntryNameCaseRule() },
            RuleV2Provider { EnumWrappingRule() },
            RuleV2Provider { ExpressionOperandWrappingRule() },
            RuleV2Provider { FilenameRule() },
            RuleV2Provider { FinalNewlineRule() },
            RuleV2Provider { FunKeywordSpacingRule() },
            RuleV2Provider { FunctionExpressionBodyRule() },
            RuleV2Provider { FunctionLiteralRule() },
            RuleV2Provider { FunctionNamingRule() },
            RuleV2Provider { FunctionReturnTypeSpacingRule() },
            RuleV2Provider { FunctionSignatureRule() },
            RuleV2Provider { FunctionStartOfBodySpacingRule() },
            RuleV2Provider { FunctionTypeModifierSpacingRule() },
            RuleV2Provider { FunctionTypeReferenceSpacingRule() },
            RuleV2Provider { IfElseBracingRule() },
            RuleV2Provider { IfElseWrappingRule() },
            RuleV2Provider { ImportOrderingRule() },
            RuleV2Provider { IndentationRule() },
            RuleV2Provider { KdocRule() },
            RuleV2Provider { KdocWrappingRule() },
            RuleV2Provider { MaxLineLengthRule() },
            RuleV2Provider { MixedConditionOperatorsRule() },
            RuleV2Provider { ModifierListSpacingRule() },
            RuleV2Provider { ModifierOrderRule() },
            RuleV2Provider { MultiLineIfElseRule() },
            RuleV2Provider { MultilineExpressionWrappingRule() },
            RuleV2Provider { MultilineLoopRule() },
            RuleV2Provider { NoBlankLineBeforeRbraceRule() },
            RuleV2Provider { NoBlankLineInListRule() },
            RuleV2Provider { NoBlankLinesInChainedMethodCallsRule() },
            RuleV2Provider { NoConsecutiveBlankLinesRule() },
            RuleV2Provider { NoConsecutiveCommentsRule() },
            RuleV2Provider { NoEmptyClassBodyRule() },
            RuleV2Provider { NoEmptyFileRule() },
            RuleV2Provider { NoEmptyFirstLineInClassBodyRule() },
            RuleV2Provider { NoEmptyFirstLineInMethodBlockRule() },
            RuleV2Provider { NoLineBreakAfterElseRule() },
            RuleV2Provider { NoLineBreakBeforeAssignmentRule() },
            RuleV2Provider { NoMultipleSpacesRule() },
            RuleV2Provider { NoSemicolonsRule() },
            RuleV2Provider { NoSingleLineBlockCommentRule() },
            RuleV2Provider { NoTrailingSpacesRule() },
            RuleV2Provider { NoUnitReturnRule() },
            RuleV2Provider { NoUnusedImportsRule() },
            RuleV2Provider { NoWildcardImportsRule() },
            RuleV2Provider { NullableTypeSpacingRule() },
            RuleV2Provider { PackageNameRule() },
            RuleV2Provider { ParameterListSpacingRule() },
            RuleV2Provider { ParameterListWrappingRule() },
            RuleV2Provider { ParameterWrappingRule() },
            RuleV2Provider { PropertyNamingRule() },
            RuleV2Provider { PropertyWrappingRule() },
            RuleV2Provider { SpacingAroundAngleBracketsRule() },
            RuleV2Provider { SpacingAroundColonRule() },
            RuleV2Provider { SpacingAroundCommaRule() },
            RuleV2Provider { SpacingAroundCurlyRule() },
            RuleV2Provider { SpacingAroundDotRule() },
            RuleV2Provider { SpacingAroundDoubleColonRule() },
            RuleV2Provider { SpacingAroundKeywordRule() },
            RuleV2Provider { SpacingAroundOperatorsRule() },
            RuleV2Provider { SpacingAroundParensRule() },
            RuleV2Provider { SpacingAroundRangeOperatorRule() },
            RuleV2Provider { SpacingAroundSquareBracketsRule() },
            RuleV2Provider { SpacingAroundUnaryOperatorRule() },
            RuleV2Provider { SpacingBetweenDeclarationsWithAnnotationsRule() },
            RuleV2Provider { SpacingBetweenDeclarationsWithCommentsRule() },
            RuleV2Provider { SpacingBetweenFunctionNameAndOpeningParenthesisRule() },
            RuleV2Provider { StatementWrappingRule() },
            RuleV2Provider { StringTemplateIndentRule() },
            RuleV2Provider { StringTemplateRule() },
            RuleV2Provider { ThenSpacingRule() },
            RuleV2Provider { TrailingCommaOnCallSiteRule() },
            RuleV2Provider { TrailingCommaOnDeclarationSiteRule() },
            RuleV2Provider { TryCatchFinallySpacingRule() },
            RuleV2Provider { TypeArgumentCommentRule() },
            RuleV2Provider { TypeArgumentListSpacingRule() },
            RuleV2Provider { TypeParameterCommentRule() },
            RuleV2Provider { TypeParameterListSpacingRule() },
            RuleV2Provider { UnnecessaryParenthesesBeforeTrailingLambdaRule() },
            RuleV2Provider { ValueArgumentCommentRule() },
            RuleV2Provider { ValueParameterCommentRule() },
            RuleV2Provider { WhenEntryBracing() },
            RuleV2Provider { WrappingRule() },
        )
}
