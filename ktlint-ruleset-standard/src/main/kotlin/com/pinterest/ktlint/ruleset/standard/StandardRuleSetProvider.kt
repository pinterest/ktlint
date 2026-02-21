package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2InstanceProvider
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
    override fun getRuleProviders(): Set<RuleV2InstanceProvider> =
        setOf(
            RuleV2InstanceProvider { AnnotationRule() },
            RuleV2InstanceProvider { AnnotationSpacingRule() },
            RuleV2InstanceProvider { ArgumentListWrappingRule() },
            RuleV2InstanceProvider { BackingPropertyNamingRule() },
            RuleV2InstanceProvider { BinaryExpressionWrappingRule() },
            RuleV2InstanceProvider { BlankLineBeforeDeclarationRule() },
            RuleV2InstanceProvider { BlankLineBetweenWhenConditions() },
            RuleV2InstanceProvider { BlockCommentInitialStarAlignmentRule() },
            RuleV2InstanceProvider { ChainMethodContinuationRule() },
            RuleV2InstanceProvider { ChainWrappingRule() },
            RuleV2InstanceProvider { ClassNamingRule() },
            RuleV2InstanceProvider { ClassSignatureRule() },
            RuleV2InstanceProvider { CommentSpacingRule() },
            RuleV2InstanceProvider { CommentWrappingRule() },
            RuleV2InstanceProvider { ContextReceiverWrappingRule() },
            RuleV2InstanceProvider { ContextReceiverListWrappingRule() },
            RuleV2InstanceProvider { EnumEntryNameCaseRule() },
            RuleV2InstanceProvider { EnumWrappingRule() },
            RuleV2InstanceProvider { ExpressionOperandWrappingRule() },
            RuleV2InstanceProvider { FilenameRule() },
            RuleV2InstanceProvider { FinalNewlineRule() },
            RuleV2InstanceProvider { FunKeywordSpacingRule() },
            RuleV2InstanceProvider { FunctionExpressionBodyRule() },
            RuleV2InstanceProvider { FunctionLiteralRule() },
            RuleV2InstanceProvider { FunctionNamingRule() },
            RuleV2InstanceProvider { FunctionReturnTypeSpacingRule() },
            RuleV2InstanceProvider { FunctionSignatureRule() },
            RuleV2InstanceProvider { FunctionStartOfBodySpacingRule() },
            RuleV2InstanceProvider { FunctionTypeModifierSpacingRule() },
            RuleV2InstanceProvider { FunctionTypeReferenceSpacingRule() },
            RuleV2InstanceProvider { IfElseBracingRule() },
            RuleV2InstanceProvider { IfElseWrappingRule() },
            RuleV2InstanceProvider { ImportOrderingRule() },
            RuleV2InstanceProvider { IndentationRule() },
            RuleV2InstanceProvider { KdocRule() },
            RuleV2InstanceProvider { KdocWrappingRule() },
            RuleV2InstanceProvider { MaxLineLengthRule() },
            RuleV2InstanceProvider { MixedConditionOperatorsRule() },
            RuleV2InstanceProvider { ModifierListSpacingRule() },
            RuleV2InstanceProvider { ModifierOrderRule() },
            RuleV2InstanceProvider { MultiLineIfElseRule() },
            RuleV2InstanceProvider { MultilineExpressionWrappingRule() },
            RuleV2InstanceProvider { MultilineLoopRule() },
            RuleV2InstanceProvider { NoBlankLineBeforeRbraceRule() },
            RuleV2InstanceProvider { NoBlankLineInListRule() },
            RuleV2InstanceProvider { NoBlankLinesInChainedMethodCallsRule() },
            RuleV2InstanceProvider { NoConsecutiveBlankLinesRule() },
            RuleV2InstanceProvider { NoConsecutiveCommentsRule() },
            RuleV2InstanceProvider { NoEmptyClassBodyRule() },
            RuleV2InstanceProvider { NoEmptyFileRule() },
            RuleV2InstanceProvider { NoEmptyFirstLineInClassBodyRule() },
            RuleV2InstanceProvider { NoEmptyFirstLineInMethodBlockRule() },
            RuleV2InstanceProvider { NoLineBreakAfterElseRule() },
            RuleV2InstanceProvider { NoLineBreakBeforeAssignmentRule() },
            RuleV2InstanceProvider { NoMultipleSpacesRule() },
            RuleV2InstanceProvider { NoSemicolonsRule() },
            RuleV2InstanceProvider { NoSingleLineBlockCommentRule() },
            RuleV2InstanceProvider { NoTrailingSpacesRule() },
            RuleV2InstanceProvider { NoUnitReturnRule() },
            RuleV2InstanceProvider { NoUnusedImportsRule() },
            RuleV2InstanceProvider { NoWildcardImportsRule() },
            RuleV2InstanceProvider { NullableTypeSpacingRule() },
            RuleV2InstanceProvider { PackageNameRule() },
            RuleV2InstanceProvider { ParameterListSpacingRule() },
            RuleV2InstanceProvider { ParameterListWrappingRule() },
            RuleV2InstanceProvider { ParameterWrappingRule() },
            RuleV2InstanceProvider { PropertyNamingRule() },
            RuleV2InstanceProvider { PropertyWrappingRule() },
            RuleV2InstanceProvider { SpacingAroundAngleBracketsRule() },
            RuleV2InstanceProvider { SpacingAroundColonRule() },
            RuleV2InstanceProvider { SpacingAroundCommaRule() },
            RuleV2InstanceProvider { SpacingAroundCurlyRule() },
            RuleV2InstanceProvider { SpacingAroundDotRule() },
            RuleV2InstanceProvider { SpacingAroundDoubleColonRule() },
            RuleV2InstanceProvider { SpacingAroundKeywordRule() },
            RuleV2InstanceProvider { SpacingAroundOperatorsRule() },
            RuleV2InstanceProvider { SpacingAroundParensRule() },
            RuleV2InstanceProvider { SpacingAroundRangeOperatorRule() },
            RuleV2InstanceProvider { SpacingAroundSquareBracketsRule() },
            RuleV2InstanceProvider { SpacingAroundUnaryOperatorRule() },
            RuleV2InstanceProvider { SpacingBetweenDeclarationsWithAnnotationsRule() },
            RuleV2InstanceProvider { SpacingBetweenDeclarationsWithCommentsRule() },
            RuleV2InstanceProvider { SpacingBetweenFunctionNameAndOpeningParenthesisRule() },
            RuleV2InstanceProvider { StatementWrappingRule() },
            RuleV2InstanceProvider { StringTemplateIndentRule() },
            RuleV2InstanceProvider { StringTemplateRule() },
            RuleV2InstanceProvider { ThenSpacingRule() },
            RuleV2InstanceProvider { TrailingCommaOnCallSiteRule() },
            RuleV2InstanceProvider { TrailingCommaOnDeclarationSiteRule() },
            RuleV2InstanceProvider { TryCatchFinallySpacingRule() },
            RuleV2InstanceProvider { TypeArgumentCommentRule() },
            RuleV2InstanceProvider { TypeArgumentListSpacingRule() },
            RuleV2InstanceProvider { TypeParameterCommentRule() },
            RuleV2InstanceProvider { TypeParameterListSpacingRule() },
            RuleV2InstanceProvider { UnnecessaryParenthesesBeforeTrailingLambdaRule() },
            RuleV2InstanceProvider { ValueArgumentCommentRule() },
            RuleV2InstanceProvider { ValueParameterCommentRule() },
            RuleV2InstanceProvider { WhenEntryBracing() },
            RuleV2InstanceProvider { WrappingRule() },
        )
}
