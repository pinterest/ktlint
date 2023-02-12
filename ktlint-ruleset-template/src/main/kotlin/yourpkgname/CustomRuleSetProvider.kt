package yourpkgname

import com.pinterest.ktlint.ruleset.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.core.api.RuleSetProviderV3

internal val CUSTOM_RULE_SET_ID = "custom-rule-set-id"

public class CustomRuleSetProvider :
    RuleSetProviderV3(CUSTOM_RULE_SET_ID) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { NoVarRule() },
        )
}
