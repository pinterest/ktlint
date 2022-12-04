package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

public const val EXPERIMENTAL_RULE_SET_ID: String = "experimental"

public class ExperimentalRuleSetProvider :
    RuleSetProviderV2(
        id = EXPERIMENTAL_RULE_SET_ID,
        about = About(
            maintainer = "KtLint",
            description = "Experimental rules based on the Kotlin coding conventions (https://kotlinlang.org/docs/coding-conventions.html) and Android Kotlin styleguide (https://developer.android.com/kotlin/style-guide). Rules are intended to be promoted to the standard ruleset once they are stable",
            license = "https://github.com/pinterest/ktlint/blob/master/LICENSE",
            repositoryUrl = "https://github.com/pinterest/ktlint",
            issueTrackerUrl = "https://github.com/pinterest/ktlint/issues",
        ),
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { UnnecessaryParenthesesBeforeTrailingLambdaRule() },
            RuleProvider { TypeParameterListSpacingRule() },
            RuleProvider { TypeArgumentListSpacingRule() },
            RuleProvider { BlockCommentInitialStarAlignmentRule() },
            RuleProvider { DiscouragedCommentLocationRule() },
            RuleProvider { FunKeywordSpacingRule() },
            RuleProvider { FunctionTypeReferenceSpacingRule() },
            RuleProvider { ModifierListSpacingRule() },
            RuleProvider { CommentWrappingRule() },
            RuleProvider { KdocWrappingRule() },
            RuleProvider { SpacingBetweenFunctionNameAndOpeningParenthesisRule() },
            RuleProvider { ParameterListSpacingRule() },
            RuleProvider { FunctionReturnTypeSpacingRule() },
            RuleProvider { FunctionStartOfBodySpacingRule() },
            RuleProvider { NullableTypeSpacingRule() },
            RuleProvider { FunctionSignatureRule() },
            RuleProvider { ContextReceiverWrappingRule() },
            RuleProvider { ClassNamingRule() },
            RuleProvider { FunctionNamingRule() },
            RuleProvider { PropertyNamingRule() },
        )
}
