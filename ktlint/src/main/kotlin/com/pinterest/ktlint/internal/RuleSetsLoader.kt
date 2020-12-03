package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.RuleSetProvider
import java.net.URLClassLoader
import java.util.ServiceLoader

/**
 * Load given list of paths to rulesets jars into map of ruleset providers.
 *
 * @return map of ruleset ids to ruleset providers
 */
internal fun JarFiles.loadRulesets(
    loadExperimental: Boolean,
    debug: Boolean,
    disabledRules: String
) = ServiceLoader
    .load(
        RuleSetProvider::class.java,
        URLClassLoader(toFilesURIList().toTypedArray())
    )
    .associateBy {
        val key = it.get().id
        // standard should go first
        if (key == "standard") "\u0000$key" else key
    }
    .filterKeys { loadExperimental || it != "experimental" }
    .filterKeys { !(disabledRules.isStandardRuleSetDisabled() && it == "\u0000standard") }
    .toSortedMap()
    .also {
        if (debug) {
            it.forEach { entry ->
                println("[DEBUG] Discovered ruleset with \"${entry.key}\" id.")
            }
        }
    }

private fun String.isStandardRuleSetDisabled() =
    this.split(",").map { it.trim() }.toSet().contains("standard")
