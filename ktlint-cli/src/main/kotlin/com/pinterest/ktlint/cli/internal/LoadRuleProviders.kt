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
    val ruleSetV2Providers = RuleSetV2Provider::class.java.loadFromKtlintCliJar({ it.id.value })
    val ruleSetV2ProviderIds = ruleSetV2Providers.map { it.id.value }

    val ruleInstanceProviders = mutableListOf<RuleInstanceProvider>()
    ruleInstanceProviders.addAll(
        ruleSetV2Providers
            .flatMap { it.getRuleProviders() }
            .toSet(),
    )

    urls
        // Remove JAR files which were provided multiple times
        .distinct()
        .forEach { url ->
            // Try to load the deprecated RulesetProviderV3 from the url. When found then print a deprecation warning
            val customRuleInstanceProviders = loadRulesetProviderV3(url, ruleSetV2ProviderIds)
            if (customRuleInstanceProviders.isNotEmpty()) {
                ruleInstanceProviders.addAll(customRuleInstanceProviders)
            } else {
                // Only when RulesetProviderV3 was not found, try to load RuleSetV2Provider, and print an error when it is not found
                ruleInstanceProviders.addAll(loadRuleSetV2Provider(url, ruleSetV2ProviderIds))
            }
        }

    return ruleInstanceProviders.toSet()
}

private fun loadRulesetProviderV3(
    url: URL,
    ruleIdsFromKtlintJars: List<String>,
): Collection<RuleInstanceProvider> =
    try {
        LOGGER.debug { "Try loading ruleset provider of type 'RuleSetProviderV3' for $url" }
        RuleSetProviderV3::class.java
            .loadFromJarFile(
                url,
                ruleIdsFromKtlintJars,
                providerId = { it.id.value },
                CustomJarProviderCheck.WARN_WHEN_DEPRECATED_PROVIDER_IS_FOUND,
            ).flatMap { it.getRuleProviders() }
            .toSet()
            .also { LOGGER.debug { "Found ${it.size} rule providers of type 'RuleSetProviderV3' for $url" } }
    } catch (t: Throwable) {
        LOGGER.warn(t) { "Cannot find ruleset providers of type 'RuleSetProviderV3' for $url" }
        emptyList()
    }

private fun loadRuleSetV2Provider(
    url: URL,
    ruleIdsFromKtlintJars: List<String>,
): Collection<RuleInstanceProvider> =
    try {
        LOGGER.debug { "Try loading ruleset provider of type 'RuleSetV2Provider' for $url" }
        RuleSetV2Provider::class.java
            .loadFromJarFile(
                url,
                ruleIdsFromKtlintJars,
                providerId = { it.id.value },
                ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING,
            ).flatMap { it.getRuleProviders() }
            .toSet()
            .also { LOGGER.debug { "Found ${it.size} rule providers of type 'RuleSetV2Provider' for $url" } }
    } catch (t: Throwable) {
        LOGGER.warn(t) { "Cannot find ruleset providers of type 'RuleSetV2Provider'" }
        emptyList()
    }
