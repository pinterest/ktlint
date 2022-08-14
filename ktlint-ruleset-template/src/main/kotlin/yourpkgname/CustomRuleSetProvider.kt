package yourpkgname

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

public class CustomRuleSetProvider :
    RuleSetProviderV2(
        id = "custom",
        about = About(
            maintainer = "KtLint",
            description = "Example of a custom rule set",
            license = "https://github.com/pinterest/ktlint/blob/master/LICENSE",
            repositoryUrl = "https://github.com/pinterest/ktlint",
            issueTrackerUrl = "https://github.com/pinterest/ktlint/issues",
        ),
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { NoVarRule() },
        )
}
