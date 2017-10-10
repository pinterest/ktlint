package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("standard",
        FinalNewlineRule(),
        ImportOrderingRule(),
        IndentationRule(),
        LambdaOutsideOfParensRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        NoItParamInMultilineLambdaRule(),
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
