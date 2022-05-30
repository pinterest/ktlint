package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule

public class StandardRuleSetProvider : RuleSetProvider {

    // Note: some of these rules may be disabled by default. See the default .editorconfig.
    override fun get(): RuleSet = RuleSet(
        "standard",
        AnnotationRule(),
        AnnotationSpacingRule(),
        ArgumentListWrappingRule(),
        ChainWrappingRule(),
        CommentSpacingRule(),
        EnumEntryNameCaseRule(),
        FilenameRule(),
        FinalNewlineRule(),
        ImportOrderingRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        MultiLineIfElseRule(),
        NoBlankLineBeforeRbraceRule(),
        NoBlankLinesInChainedMethodCallsRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        NoEmptyFirstLineInMethodBlockRule(),
        NoLineBreakAfterElseRule(),
        NoLineBreakBeforeAssignmentRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnitReturnRule(),
        NoUnusedImportsRule(),
        NoWildcardImportsRule(),
        PackageNameRule(),
        ParameterListWrappingRule(),
        SpacingAroundAngleBracketsRule(),
        SpacingAroundColonRule(),
        SpacingAroundCommaRule(),
        SpacingAroundCurlyRule(),
        SpacingAroundDotRule(),
        SpacingAroundDoubleColonRule(),
        SpacingAroundKeywordRule(),
        SpacingAroundOperatorsRule(),
        SpacingAroundParensRule(),
        SpacingAroundRangeOperatorRule(),
        SpacingAroundUnaryOperatorRule(),
        SpacingBetweenDeclarationsWithAnnotationsRule(),
        SpacingBetweenDeclarationsWithCommentsRule(),
        StringTemplateRule(),
        TrailingCommaRule(),
        WrappingRule()
    )
}
