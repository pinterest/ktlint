package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("standard",
        FinalNewlineRule(),
        // disabled until it's clear how to reconcile difference in Intellij & Android Studio import layout
        // ImportOrderingRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        // disabled until it's clear what to do in case of `import _.it`
        // NoItParamInMultilineLambdaRule(),
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
        StringTemplateRule()
    )

}
