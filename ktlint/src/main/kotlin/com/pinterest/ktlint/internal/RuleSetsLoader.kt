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
    .also { ruleSetMap ->
        if (debug) {
            ruleSetMap.forEach { entry ->
                System.err.println("[DEBUG] Discovered ruleset with \"${entry.key}\" id.")
            }
        }

        val expectedNumberOfCustomRuleSetsToBeLoaded = this.distinct().count()
        val customRuleSetsLoaded =
            ruleSetMap
                .filterKeys { it != "experimental" && it != "\u0000standard" }
                .values
                .map { it.javaClass.canonicalName }
        val actualNumberOfCustomRuleSetsLoaded = customRuleSetsLoaded.count()
        if (expectedNumberOfCustomRuleSetsToBeLoaded != actualNumberOfCustomRuleSetsLoaded) {
            System.err.println(
                """
                [WARNING] Number of custom rule sets loaded does not match the expected number of rule sets to be loaded.
                          Expected to load $expectedNumberOfCustomRuleSetsToBeLoaded custom rule set(s) for rules: ${this.joinToString()}
                          Actually loaded $actualNumberOfCustomRuleSetsLoaded custom rule set(s) with names: ${customRuleSetsLoaded.joinToString() }
                          One or more of the specified jars does not provide the custom rule set correctly. Check following:
                            - Does the jar contain an implementation of the RuleSetProvider interface?
                            - Does the jar contain a resource file with name "com.pinterest.ktlint.core.RuleSetProvider"?
                            - Is the resource file located in directory "src/main/resources/META-INF/services"?
                            - Does the resource file contain the fully qualified class name of the class implementing the RuleSetProvider interface?
                """.trimIndent() // ktlint-disable string-template
            )
        }
    }

private fun String.isStandardRuleSetDisabled() =
    this.split(",").map { it.trim() }.toSet().contains("standard")
