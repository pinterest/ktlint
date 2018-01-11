package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("standard",
        ChainWrappingRule(),
        FinalNewlineRule(),
        // disabled until it's clear how to reconcile difference in Intellij & Android Studio import layout
        // ImportOrderingRule(),
        NoLineBreakAfterElseRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoBlankLineBeforeRbraceRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        // disabled until it's clear what to do in case of `import _.it`
        // NoItParamInMultilineLambdaRule(),
        NoLineBreakBeforeAssignmentRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnitReturnRule(),
        NoUnusedImportsRule(),
        NoWildcardImportsRule(),
        SpacingAroundColonRule(),
        SpacingAroundCommaRule(),
        SpacingAroundCurlyRule(),
        SpacingAroundKeywordRule(),
        SpacingAroundOperatorsRule(),
        SpacingAroundRangeOperatorRule(),
        StringTemplateRule()
    )
}
