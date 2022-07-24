package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule

public class StandardRuleSetProvider :
    RuleSetProviderV2(
        id = "standard",
        about = About(
            maintainer = "KtLint",
            description = "Standard rules based on the Kotlin coding conventions (https://kotlinlang.org/docs/coding-conventions.html) and Android Kotlin styleguide (https://developer.android.com/kotlin/style-guide)",
            license = "https://github.com/pinterest/ktlint/blob/master/LICENSE",
            repositoryUrl = "https://github.com/pinterest/ktlint",
            issueTrackerUrl = "https://github.com/pinterest/ktlint/issues"
        )
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { AnnotationRule() },
            RuleProvider { AnnotationSpacingRule() },
            RuleProvider { ArgumentListWrappingRule() },
            RuleProvider { ChainWrappingRule() },
            RuleProvider { CommentSpacingRule() },
            RuleProvider { EnumEntryNameCaseRule() },
            RuleProvider { FilenameRule() },
            RuleProvider { FinalNewlineRule() },
            RuleProvider { ImportOrderingRule() },
            RuleProvider { IndentationRule() },
            RuleProvider { MaxLineLengthRule() },
            RuleProvider { ModifierOrderRule() },
            RuleProvider { MultiLineIfElseRule() },
            RuleProvider { NoBlankLineBeforeRbraceRule() },
            RuleProvider { NoBlankLinesInChainedMethodCallsRule() },
            RuleProvider { NoConsecutiveBlankLinesRule() },
            RuleProvider { NoEmptyClassBodyRule() },
            RuleProvider { NoEmptyFirstLineInMethodBlockRule() },
            RuleProvider { NoLineBreakAfterElseRule() },
            RuleProvider { NoLineBreakBeforeAssignmentRule() },
            RuleProvider { NoMultipleSpacesRule() },
            RuleProvider { NoSemicolonsRule() },
            RuleProvider { NoTrailingSpacesRule() },
            RuleProvider { NoUnitReturnRule() },
            RuleProvider { NoUnusedImportsRule() },
            RuleProvider { NoWildcardImportsRule() },
            RuleProvider { PackageNameRule() },
            RuleProvider { ParameterListWrappingRule() },
            RuleProvider { SpacingAroundAngleBracketsRule() },
            RuleProvider { SpacingAroundColonRule() },
            RuleProvider { SpacingAroundCommaRule() },
            RuleProvider { SpacingAroundCurlyRule() },
            RuleProvider { SpacingAroundDotRule() },
            RuleProvider { SpacingAroundDoubleColonRule() },
            RuleProvider { SpacingAroundKeywordRule() },
            RuleProvider { SpacingAroundOperatorsRule() },
            RuleProvider { SpacingAroundParensRule() },
            RuleProvider { SpacingAroundRangeOperatorRule() },
            RuleProvider { SpacingAroundUnaryOperatorRule() },
            RuleProvider { SpacingBetweenDeclarationsWithAnnotationsRule() },
            RuleProvider { SpacingBetweenDeclarationsWithCommentsRule() },
            RuleProvider { StringTemplateRule() },
            RuleProvider { TrailingCommaRule() },
            RuleProvider { WrappingRule() }
        )
}
