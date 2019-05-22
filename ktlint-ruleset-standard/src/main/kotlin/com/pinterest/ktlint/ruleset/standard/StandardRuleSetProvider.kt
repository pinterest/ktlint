package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

class StandardRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "standard",
        // disabled ("./mvnw clean verify" fails with "Internal Error")
        // AnnotationRule(),
        ChainWrappingRule(),
        CommentSpacingRule(),
        FilenameRule(),
        FinalNewlineRule(),
        // disabled until there is a way to suppress rules globally (https://git.io/fhxnm)
        // PackageNameRule(),
        // disabled until auto-correct is working properly
        // (e.g. try formatting "if (true)\n    return { _ ->\n        _\n}")
        // MultiLineIfElseRule(),
        IndentationRule(),
        MaxLineLengthRule(),
        ModifierOrderRule(),
        NoBlankLineBeforeRbraceRule(),
        NoConsecutiveBlankLinesRule(),
        NoEmptyClassBodyRule(),
        // disabled until it's clear what to do in case of `import _.it`
        // NoItParamInMultilineLambdaRule(),
        NoLineBreakAfterElseRule(),
        NoLineBreakBeforeAssignmentRule(),
        NoMultipleSpacesRule(),
        NoSemicolonsRule(),
        NoTrailingSpacesRule(),
        NoUnitReturnRule(),
        NoUnusedImportsRule(),
        // Disabling because it is now allowed by the Jetbrains styleguide, although it is still disallowed by
        // the Android styleguide.
        // Re-enable when there is a way to globally disable rules
        // See discussion here: https://github.com/pinterest/ktlint/issues/48
        // NoWildcardImportsRule(),
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
