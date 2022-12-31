package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import com.pinterest.ktlint.core.initKtLintKLogger
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Loads given list of paths to jar files. For files containing a [RuleSetProviderV2] class, get all [RuleProvider]s.
 */
internal fun List<URL>.loadRuleProviders(debug: Boolean): Set<RuleProvider> =
    getKtlintRulesets()
        .plus(
            getRuleProvidersFromCustomRuleSetJars(debug),
        ).values
        .flatten()
        .toSet()

private fun getKtlintRulesets(): Map<String, Set<RuleProvider>> {
    return loadRulesetsFrom()
}

private fun loadRulesetsFrom(url: URL? = null): Map<String, Set<RuleProvider>> =
    try {
        ServiceLoader
            .load(
                RuleSetProviderV2::class.java,
                URLClassLoader(listOfNotNull(url).toTypedArray()),
            ).associate { ruleSetProviderV2 -> ruleSetProviderV2.id to ruleSetProviderV2.getRuleProviders() }
    } catch (e: ServiceConfigurationError) {
        LOGGER.warn { "Error while loading rule set JAR '$url':\n${e.printStackTrace()}" }
        emptyMap()
    }

private fun List<URL>.getRuleProvidersFromCustomRuleSetJars(debug: Boolean): Map<String, Set<RuleProvider>> =
    this
        // Remove JAR files which were provided multiple times
        .distinct()
        .flatMap { getRuleProvidersFromCustomRuleSetJar(it, debug).entries }
        .associate { it.key to it.value }

private fun getRuleProvidersFromCustomRuleSetJar(
    url: URL,
    debug: Boolean,
): Map<String, Set<RuleProvider>> {
    if (debug) {
        LOGGER.debug { "JAR ruleset provided with path \"${url.path}\"" }
    }
    return loadRulesetsFrom(url)
        .filterKeys {
            // Ignore the Ktlint rule sets when they are included in the custom rule set.
            it !in KTLINT_RULE_SETS
        }.also { ruleSetIdMap ->
            if (ruleSetIdMap.isEmpty()) {
                LOGGER.warn {
                    """
                    JAR ${url.path}, provided as command line argument, does not contain a custom ruleset provider.
                        Check following:
                          - Does the jar contain an implementation of the RuleSetProviderV2 interface?
                          - Does the jar contain a resource file with name "com.pinterest.ktlint.core.RuleSetProviderV2"?
                          - Is the resource file located in directory "src/main/resources/META-INF/services"?
                          - Does the resource file contain the fully qualified class name of the class implementing the RuleSetProviderV2 interface?
                    """.trimIndent()
                }
            }
        }
}

private val KTLINT_RULE_SETS = listOf("standard", "experimental")
