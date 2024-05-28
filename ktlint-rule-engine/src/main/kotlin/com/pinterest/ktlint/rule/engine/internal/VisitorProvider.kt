package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
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
     * The set of [RuleProvider]s to be executed. This set should not contain any [RuleProvider]s which are disabled via the [EditorConfig].
     */
    ruleProviders: Set<RuleProvider>,
    /**
     * Creates a new [RuleProviderSorter]. Only to be used in unit tests where the same set of rules are used with distinct
     * [Rule.VisitorModifier]s.
     */
    recreateRuleSorter: Boolean = false,
) {
    /**
     * The list of [ruleProvidersSorted] is sorted based on the [Rule.VisitorModifier] of the rules.
     */
    private val ruleProvidersSorted: List<RuleProvider> =
        if (recreateRuleSorter) {
            RuleProviderSorter()
        } else {
            RULE_PROVIDER_SORTER
        }.getSortedRuleProviders(ruleProviders)

    internal val rules: List<Rule>
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
