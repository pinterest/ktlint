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
    this
        .plus(
            // Ensure that always at least one element exists in this list so that the rule sets provided by the KtLint
            // CLI module itself will be found even in case no JAR files are specified
            null,
        )
        // Remove JAR files which were provided multiple times
        .distinct()
        .flatMap { getRuleProvidersFromJar(it, debug).values }
        .flatten()
        .toSet()

private fun getRuleProvidersFromJar(
    url: URL?,
    debug: Boolean,
): Map<String, Set<RuleProvider>> {
    if (url != null && debug) {
        LOGGER.debug { "JAR ruleset provided with path \"${url.path}\"" }
    }
    return try {
        ServiceLoader
            .load(
                RuleSetProviderV2::class.java,
                URLClassLoader(listOfNotNull(url).toTypedArray()),
            ).filter {
                // The KtLint-root (CLI) module includes the standard rule set. When this rule set is also included in the specified JAR
                // (url != null) then ignore the rule set.
                url == null || it.id != "standard"
            }.associate { ruleSetProviderV2 -> ruleSetProviderV2.id to ruleSetProviderV2.getRuleProviders() }
            .also { ruleSetIdMap ->
                if (url != null && ruleSetIdMap.isEmpty()) {
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
    } catch (e: ServiceConfigurationError) {
        emptyMap()
    }
}
