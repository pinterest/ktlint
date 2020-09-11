package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

public class ExperimentalRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "experimental",
        AnnotationRule(),
        ArgumentListWrappingRule(),
        MultiLineIfElseRule(),
        NoEmptyFirstLineInMethodBlockRule(),
        PackageNameRule(),
        EnumEntryNameCaseRule(),
        SpacingAroundDoubleColonRule(),
        SpacingBetweenDeclarationsWithCommentsRule(),
        SpacingBetweenDeclarationsWithAnnotationsRule(),
        SpacingAroundAngleBracketsRule(),
        SpacingAroundUnaryOperatorRule(),
        AnnotationSpacingRule()
    )
}
