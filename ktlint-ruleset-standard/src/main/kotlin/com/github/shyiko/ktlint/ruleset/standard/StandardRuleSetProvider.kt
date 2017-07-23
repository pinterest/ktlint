package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("standard",
        FinalNewlineRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnitReturnRule(),
        NoUnusedImportsRule(),
        NoWildcardImportsRule(),
        SpacingAroundCommaRule(),
        SpacingAroundKeywordRule(),
        SpacingAroundColonRule(),
        SpacingAroundCurlyRule(),
        SpacingAroundOperatorsRule(),
        StringTemplateRule()
    )

}
