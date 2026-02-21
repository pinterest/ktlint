package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Loads given list of paths to jar files.
 */
internal fun loadRuleProviders(urls: List<URL>): Set<RuleInstanceProvider> {
    val ruleProviders = mutableListOf<RuleInstanceProvider>()
    try {
        LOGGER.debug { "Try loading ruleset provider of type 'RuleSetV2Provider' found for $urls" }
        ruleProviders.addAll(
            RuleSetV2Provider::class.java
                .loadFromJarFiles(urls, providerId = { it.id.value }, ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING)
                .flatMap { it.getRuleProviders() }
                .toSet()
                .also { LOGGER.debug { "Found ${it.size} rule providers of type 'RuleSetV2Provider' found for $urls" } },
        )
    } catch (t: Throwable) {
        LOGGER.warn(t) { "Cannot find ruleset providers of type 'RuleSetV2Provider'" }
    }
    try {
        LOGGER.debug { "Try loading ruleset provider of type 'RuleSetProviderV3' found for $urls" }
        ruleProviders.addAll(
            RuleSetProviderV3::class.java
                .loadFromJarFiles(urls, providerId = { it.id.value }, CustomJarProviderCheck.ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND)
                .flatMap { it.getRuleProviders() }
                .toSet()
                .also { LOGGER.debug { "Found ${it.size} rule providers of type 'RuleSetProviderV3' found for $urls" } },
        )
    } catch (t: Throwable) {
        LOGGER.warn(t) { "Cannot find ruleset providers of type 'RuleSetProviderV3'" }
    }
    return ruleProviders.toSet()
}
