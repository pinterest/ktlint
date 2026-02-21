package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleV2InstanceProvider
import com.pinterest.ktlint.rule.engine.internal.rules.KtlintSuppressionRule
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Add internal [RuleInstanceProvider]s. These rule providers always have to run regardless of the rules providers which are provided by the
 * API consumer. In case the API consumer tries to provide a rule with the same rule id as an internal rule provider than it will be
 * ignored.
 */
internal class InternalRuleProvidersFilter(
    private val ktLintRuleEngine: KtLintRuleEngine,
) : RuleFilter {
    private val internalRuleProviders =
        setOf(
            RuleV2InstanceProvider {
                KtlintSuppressionRule(
                    ktLintRuleEngine.ruleProviders.map { it.ruleId },
                )
            },
        )

    override fun filter(ruleProviders: Set<RuleInstanceProvider>): Set<RuleInstanceProvider> {
        val internalRuleIds = internalRuleProviders.map { it.ruleId }
        return ruleProviders
            .mapNotNullTo(mutableSetOf()) {
                if (it.ruleId in internalRuleIds) {
                    LOGGER.error { "The provided rule with id '${it.ruleId}' is ignored in favour of Ktlint's rule with same id" }
                    null
                } else {
                    it
                }
            }.plus(internalRuleProviders)
            .toSet()
    }
}
