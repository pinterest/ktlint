package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

public class StandardRuleSetProvider : RuleSetProvider {

    // Note: some of these rules may be disabled by default. See the default .editorconfig.
    override fun get(): RuleSet = RuleSet(
        "standard",
        ChainWrappingRule(),
        CommentSpacingRule(),
        FilenameRule(),
        FinalNewlineRule(),
        ImportOrderingRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoBlankLineBeforeRbraceRule(),
        NoConsecutiveBlankLinesRule(),
        NoBlankLinesInChainedMethodCallsRule(),
        NoEmptyClassBodyRule(),
        NoLineBreakAfterElseRule(),
        NoLineBreakBeforeAssignmentRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnitReturnRule(),
        NoUnusedImportsRule(),
        NoWildcardImportsRule(),
        ParameterListWrappingRule(),
        SpacingAroundColonRule(),
        SpacingAroundCommaRule(),
        SpacingAroundCurlyRule(),
        SpacingAroundDotRule(),
        SpacingAroundKeywordRule(),
        SpacingAroundOperatorsRule(),
        SpacingAroundParensRule(),
        SpacingAroundRangeOperatorRule(),
        StringTemplateRule(),
        WrappingRule()
    )
}
