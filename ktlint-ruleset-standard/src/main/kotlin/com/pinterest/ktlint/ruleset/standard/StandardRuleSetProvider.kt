package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "standard",
        AnnotationRule(),
        ChainWrappingRule(),
        CommentSpacingRule(),
        FilenameRule(),
        FinalNewlineRule(),
        PackageNameRule(),
        MultiLineIfElseRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoBlankLineBeforeRbraceRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        NoItParamInMultilineLambdaRule(),
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
        StringTemplateRule()
    )
}
