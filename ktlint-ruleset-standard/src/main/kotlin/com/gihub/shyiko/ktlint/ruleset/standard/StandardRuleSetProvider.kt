package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("standard",
        IndentationRule(),
        NoConsecutiveBlankLinesRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnusedImportsRule(),
        NoWildcardImportsRule(),
        SpacingAfterCommaRule(),
        SpacingAfterKeywordRule(),
        SpacingAroundColonRule(),
        SpacingAroundCurlyRule(),
        SpacingAroundOperatorsRule(),
        CapitalizedClassNamesRule()
    )

}
