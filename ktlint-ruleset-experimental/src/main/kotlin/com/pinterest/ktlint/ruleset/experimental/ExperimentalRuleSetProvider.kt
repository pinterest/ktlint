package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

class ExperimentalRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "experimental",
        AnnotationRule(),
        IndentationRule(),
        MultiLineIfElseRule(),
        NoEmptyFirstLineInMethodBlockRule(),
        PackageNameRule(),
        EnumEntryNameCaseRule(),
        SpacingAroundDoubleColonRule(),
        SpacingBetweenDeclarationsWithCommentsRule(),
        SpacingBetweenDeclarationsWithAnnotationsRule()
    )
}
