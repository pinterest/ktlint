package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.RuleSetProvider
import java.net.URLClassLoader
import java.util.ServiceLoader

/**
 * Load given list of paths to rulesets jars into map of ruleset providers.
 *
 * @return map of ruleset ids to ruleset providers
 */
internal fun List<String>.loadRulesets(
    loadExperimental: Boolean,
    debug: Boolean
) = ServiceLoader
    .load(
        RuleSetProvider::class.java,
        URLClassLoader(this.toFilesURIList().toTypedArray())
    )
    .associateBy {
        val key = it.get().id
        // standard should go first
        if (key == "standard") "\u0000$key" else key
    }
    .filterKeys { loadExperimental || it != "experimental" }
    .toSortedMap()
    .also {
        if (debug) {
            it.forEach { entry ->
                println("[DEBUG] Discovered ruleset with \"${entry.key}\" id.")
            }
        }
    }
