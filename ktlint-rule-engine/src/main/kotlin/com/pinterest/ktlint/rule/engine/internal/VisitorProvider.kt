package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * The VisitorProvider is created for each file being scanned. As the [RuleProviderSorter] logs the order in which the
 * rules are executed, a singleton instance of the class is used to prevent that the logs are flooded with duplicate
 * log lines.
 */
private val RULE_PROVIDER_SORTER = RuleProviderSorter()

internal class VisitorProvider(
    /**
     * The set of [RuleInstanceProvider]s to be executed. This set should not contain any [RuleInstanceProvider]s which are disabled via the
     * [EditorConfig].
     */
    ruleProviders: Set<RuleInstanceProvider>,
    /**
     * Creates a new [RuleProviderSorter]. Only to be used in unit tests where the same set of rules are used with distinct
     * [RuleV2.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false,
) {
    /**
     * The list of [ruleProvidersSorted] is sorted based on the [RuleV2.VisitorModifier] of the rules.
     */
    private val ruleProvidersSorted: List<RuleInstanceProvider> =
        if (recreateRuleSorter) {
            RuleProviderSorter()
        } else {
            RULE_PROVIDER_SORTER
        }.getSortedRuleProviders(ruleProviders)

    internal val rules: List<RuleV2>
        get() {
            if (ruleProvidersSorted.isEmpty()) {
                LOGGER.debug { "Skipping file as no enabled rules are found to be executed" }
                return emptyList()
            }
            return ruleProvidersSorted.map {
                it.createNewRuleInstance()
            }
        }
}
