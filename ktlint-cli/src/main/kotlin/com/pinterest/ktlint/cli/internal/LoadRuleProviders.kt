package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.net.URL

/**
 * Loads given list of paths to jar files. For files containing a [RuleSetProviderV3] class, get all [RuleProvider]s.
 */
internal fun loadRuleProviders(urls: List<URL>): Set<RuleProvider> {
    // Keep code around as example for future deprecation of current RuleSetProvider
    // An error about finding a deprecated RuleSetProviderV2 is more important than reporting an error about a missing RuleSetProviderV3
    // RuleSetProviderV2::class.java.loadFromJarFiles(urls, providerId = { it.id }, ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND)

    return RuleSetProviderV3::class.java
        .loadFromJarFiles(urls, providerId = { it.id.value }, ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING)
        .flatMap { it.getRuleProviders() }
        .toSet()
}
