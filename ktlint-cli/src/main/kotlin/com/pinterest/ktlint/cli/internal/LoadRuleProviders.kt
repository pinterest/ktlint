package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV1Provider
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Loads given list of paths to jar files. For files containing a [RuleSetV1Provider] class, get all [RuleProvider]s.
 */
internal fun loadRuleProviders(urls: List<URL>): Set<RuleInstanceProvider> {
    // Keep code around as example for future deprecation of current RuleSetProvider
    // An error about finding a deprecated RuleSetProviderV2 is more important than reporting an error about a missing RuleSetV1Provider
    // RuleSetProviderV2::class.java.loadFromJarFiles(urls, providerId = { it.id }, ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND)

    // TODO: Process per jar for find RuleSetV2 versus RuleSetProviderV3
    try {
        LOGGER.debug { "Try loading ruleset provider of type 'RuleSetV2Provider'" }
        val ruleProviders =
            RuleSetV2Provider::class.java
                .loadFromJarFiles(urls, providerId = { it.id.value }, ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING)
                .flatMap { it.getRuleProviders() }
                .toSet()
        if (ruleProviders.isNotEmpty()) {
            return ruleProviders
        }
        LOGGER.debug { "No rule providers of type 'RuleSetV2Provider' found for $urls" }
    } catch (t: Throwable) {
        LOGGER.warn { "Cannot find ruleset providers of type 'RuleSetV2Provider'" }
    }

    return try {
        LOGGER.debug { "Try loading fallback ruleset provider of type 'RuleSetProviderV3'" }
        RuleSetProviderV3::class.java
            .loadFromJarFiles(urls, providerId = { it.id.value }, CustomJarProviderCheck.ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND)
            .flatMap { it.getRuleProviders() }
            .toSet()
    } catch (t: Throwable) {
        emptySet()
    }
}
