package io.github.ktlint.core.ruleset.standard

import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
import io.github.ktlint.core.rule.engine.core.api.RuleSetId
import io.github.ktlint.core.rule.engine.core.api.RuleV2InstanceProvider
import io.github.ktlint.core.ruleset.standard.rules.AnnotationRule
import io.github.ktlint.core.ruleset.standard.rules.AnnotationSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.ArgumentListWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.BackingPropertyNamingRule
import io.github.ktlint.core.ruleset.standard.rules.BinaryExpressionWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.BlankLineBeforeDeclarationRule
import io.github.ktlint.core.ruleset.standard.rules.BlankLineBeforeFileAnnotation
import io.github.ktlint.core.ruleset.standard.rules.BlankLineBeforeImports
import io.github.ktlint.core.ruleset.standard.rules.BlankLineBeforePackage
import io.github.ktlint.core.ruleset.standard.rules.BlankLineBetweenWhenConditions
import io.github.ktlint.core.ruleset.standard.rules.BlockCommentInitialStarAlignmentRule
import io.github.ktlint.core.ruleset.standard.rules.CallExpressionWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ChainMethodContinuationRule
import io.github.ktlint.core.ruleset.standard.rules.ChainWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ClassNamingRule
import io.github.ktlint.core.ruleset.standard.rules.ClassSignatureRule
import io.github.ktlint.core.ruleset.standard.rules.CommentSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.CommentWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ContextReceiverListWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ContextReceiverWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.EnumEntryNameCaseRule
import io.github.ktlint.core.ruleset.standard.rules.EnumWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ExpressionOperandWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.FilenameRule
import io.github.ktlint.core.ruleset.standard.rules.FinalNewlineRule
import io.github.ktlint.core.ruleset.standard.rules.FunKeywordSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionExpressionBodyRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionLiteralRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionNamingRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionReturnTypeSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionSignatureRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionStartOfBodySpacingRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionTypeModifierSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.FunctionTypeReferenceSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.IfElseBracingRule
import io.github.ktlint.core.ruleset.standard.rules.IfElseWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ImportOrderingRule
import io.github.ktlint.core.ruleset.standard.rules.IndentationRule
import io.github.ktlint.core.ruleset.standard.rules.KdocRule
import io.github.ktlint.core.ruleset.standard.rules.KdocWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.LambdaReturnRule
import io.github.ktlint.core.ruleset.standard.rules.MaxLineLengthRule
import io.github.ktlint.core.ruleset.standard.rules.MixedConditionOperatorsRule
import io.github.ktlint.core.ruleset.standard.rules.ModifierListSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.ModifierOrderRule
import io.github.ktlint.core.ruleset.standard.rules.MultiLineIfElseRule
import io.github.ktlint.core.ruleset.standard.rules.MultilineExpressionWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.MultilineLoopRule
import io.github.ktlint.core.ruleset.standard.rules.NoBlankLineBeforeRbraceRule
import io.github.ktlint.core.ruleset.standard.rules.NoBlankLineInListRule
import io.github.ktlint.core.ruleset.standard.rules.NoBlankLinesInChainedMethodCallsRule
import io.github.ktlint.core.ruleset.standard.rules.NoConsecutiveBlankLinesRule
import io.github.ktlint.core.ruleset.standard.rules.NoConsecutiveCommentsRule
import io.github.ktlint.core.ruleset.standard.rules.NoEmptyClassBodyRule
import io.github.ktlint.core.ruleset.standard.rules.NoEmptyFileRule
import io.github.ktlint.core.ruleset.standard.rules.NoEmptyFirstLineInClassBodyRule
import io.github.ktlint.core.ruleset.standard.rules.NoEmptyFirstLineInMethodBlockRule
import io.github.ktlint.core.ruleset.standard.rules.NoLineBreakAfterElseRule
import io.github.ktlint.core.ruleset.standard.rules.NoLineBreakBeforeAssignmentRule
import io.github.ktlint.core.ruleset.standard.rules.NoMultipleSpacesRule
import io.github.ktlint.core.ruleset.standard.rules.NoSemicolonsRule
import io.github.ktlint.core.ruleset.standard.rules.NoSingleLineBlockCommentRule
import io.github.ktlint.core.ruleset.standard.rules.NoTrailingSpacesRule
import io.github.ktlint.core.ruleset.standard.rules.NoUnitReturnRule
import io.github.ktlint.core.ruleset.standard.rules.NoUnusedImportsRule
import io.github.ktlint.core.ruleset.standard.rules.NoWildcardImportsRule
import io.github.ktlint.core.ruleset.standard.rules.NullableTypeSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.PackageImportSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.PackageNameRule
import io.github.ktlint.core.ruleset.standard.rules.ParameterListSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.ParameterListWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.ParameterWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.PropertyNamingRule
import io.github.ktlint.core.ruleset.standard.rules.PropertyWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundAngleBracketsRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundColonRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundCommaRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundCurlyRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundDotRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundDoubleColonRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundKeywordRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundOperatorsRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundParensRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundRangeOperatorRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundSquareBracketsRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingAroundUnaryOperatorRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingBetweenDeclarationsWithAnnotationsRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingBetweenDeclarationsWithCommentsRule
import io.github.ktlint.core.ruleset.standard.rules.SpacingBetweenFunctionNameAndOpeningParenthesisRule
import io.github.ktlint.core.ruleset.standard.rules.StatementWrappingRule
import io.github.ktlint.core.ruleset.standard.rules.StringTemplateIndentRule
import io.github.ktlint.core.ruleset.standard.rules.StringTemplateRule
import io.github.ktlint.core.ruleset.standard.rules.ThenSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.TrailingCommaOnCallSiteRule
import io.github.ktlint.core.ruleset.standard.rules.TrailingCommaOnDeclarationSiteRule
import io.github.ktlint.core.ruleset.standard.rules.TryCatchFinallySpacingRule
import io.github.ktlint.core.ruleset.standard.rules.TypeArgumentCommentRule
import io.github.ktlint.core.ruleset.standard.rules.TypeArgumentListSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.TypeParameterCommentRule
import io.github.ktlint.core.ruleset.standard.rules.TypeParameterListSpacingRule
import io.github.ktlint.core.ruleset.standard.rules.UnnecessaryParenthesesBeforeTrailingLambdaRule
import io.github.ktlint.core.ruleset.standard.rules.ValueArgumentCommentRule
import io.github.ktlint.core.ruleset.standard.rules.ValueParameterCommentRule
import io.github.ktlint.core.ruleset.standard.rules.WhenEntryBracing
import io.github.ktlint.core.ruleset.standard.rules.WrappingRule

public class StandardRuleSetProvider : RuleSetV2Provider(RuleSetId.STANDARD) {
    override fun getRuleProviders(): Set<RuleV2InstanceProvider> =
        setOf(
            RuleV2InstanceProvider { AnnotationRule() },
            RuleV2InstanceProvider { AnnotationSpacingRule() },
            RuleV2InstanceProvider { ArgumentListWrappingRule() },
            RuleV2InstanceProvider { BackingPropertyNamingRule() },
            RuleV2InstanceProvider { BinaryExpressionWrappingRule() },
            RuleV2InstanceProvider { BlankLineBeforeDeclarationRule() },
            RuleV2InstanceProvider { BlankLineBeforeFileAnnotation() },
            RuleV2InstanceProvider { BlankLineBeforeImports() },
            RuleV2InstanceProvider { BlankLineBeforePackage() },
            RuleV2InstanceProvider { BlankLineBetweenWhenConditions() },
            RuleV2InstanceProvider { BlockCommentInitialStarAlignmentRule() },
            RuleV2InstanceProvider { CallExpressionWrappingRule() },
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
            RuleV2InstanceProvider { LambdaReturnRule() },
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
            RuleV2InstanceProvider { PackageImportSpacingRule() },
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
