package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.core.initKtLintKLogger
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.SortedMap
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Load given list of paths to ruleset jars into map of ruleset providers.
 *
 * @return map of ruleset ids to ruleset providers
 */
internal fun JarFiles.loadRulesets(
    loadExperimental: Boolean,
    debug: Boolean,
    disabledRules: String
): SortedMap<String, RuleSetProvider> {
    return toFilesURIList()
        .plus(
            // Ensure that always at least one element exists in this list so that the rules ets provided by ktlint will
            // by ktlint will be found even in case no JAR files are specified
            null
        )
        // Remove JAR files which were provided multiple times
        .distinct()
        .map { getRuleSetProvidersByUrl(it, debug) }
        .flatMap { (url, ruleSetProviders) ->
            if (url != null) {
                reportWhenMissingCustomRuleSetProvider(url, ruleSetProviders)
            }
            ruleSetProviders
        }
        // Remove duplicate rule set providers (each url loaded contains all rulesets provided by ktlint)
        .distinct()
        .associateBy { it.get().id }
        .filterKeys { loadExperimental || it != "experimental" }
        .filterKeys { !(disabledRules.isStandardRuleSetDisabled() && it == "standard") }
        .toSortedMap()
}

private fun getRuleSetProvidersByUrl(
    url: URL?,
    debug: Boolean
): Pair<URL?, List<RuleSetProvider>> {
    if (url != null && debug) {
        logger.debug { "JAR ruleset provided with path \"${url.path}\"" }
    }
    val ruleSetProviders = ServiceLoader.load(
        RuleSetProvider::class.java,
        URLClassLoader(listOfNotNull(url).toTypedArray())
    ).toList()
    return url to ruleSetProviders.toList()
}

private fun reportWhenMissingCustomRuleSetProvider(
    url: URL,
    ruleSetProviders: List<RuleSetProvider>
) {
    val hasCustomRuleSetProviders =
        ruleSetProviders
            .filterNot { it.get().id == "standard" }
            .filterNot { it.get().id == "experimental" }
            .any()
    if (!hasCustomRuleSetProviders) {
        logger.warn {
            """
            JAR ${url.path}, provided as command line argument, does not contain a custom ruleset provider.
                Check following:
                  - Does the jar contain an implementation of the RuleSetProvider interface?
                  - Does the jar contain a resource file with name "com.pinterest.ktlint.core.RuleSetProvider"?
                  - Is the resource file located in directory "src/main/resources/META-INF/services"?
                  - Does the resource file contain the fully qualified class name of the class implementing the RuleSetProvider interface?
            """.trimIndent() // ktlint-disable string-template
        }
    }
}

private fun String.isStandardRuleSetDisabled() =
    this.split(",").map { it.trim() }.toSet().contains("standard")
