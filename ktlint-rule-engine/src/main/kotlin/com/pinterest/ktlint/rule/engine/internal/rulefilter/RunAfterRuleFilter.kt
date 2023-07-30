package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RunAfterRuleFilter.RunAfterRuleOrderModifier.ADD
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RunAfterRuleFilter.RunAfterRuleOrderModifier.BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RunAfterRuleFilter.RunAfterRuleOrderModifier.IGNORE
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RunAfterRuleFilter.RunAfterRuleOrderModifier.REQUIRED_RUN_AFTER_RULE_NOT_LOADED
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Creates a filter that exclude rules that have a [Rule.VisitorModifier.RunAfterRule] declared with mode
 * [ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED] for a rule id which is not loaded or not enabled. This filter also removes
 * [Rule.VisitorModifier.RunAfterRule] for rules that are not loaded or are loaded but not enabled.
 */
internal class RunAfterRuleFilter : RuleFilter {
    // List of rule ids that are to be filtered
    private val ruleIdsToBeFiltered = mutableSetOf<RuleId>()

    // The list of rule Providers that still need to be processed
    private val unprocessedRuleProviders = mutableSetOf<RuleProvider>()

    // The list of rule Providers for which it has been determined that one of following is true:
    //  - The rule is not depending on another rule to be run first
    //  - The rule is depending on another rule to be run first, but for which it is not required that that rule is actually enabled
    //  - The rule is depending on another rule to be run first, and it has been determined that that rule is actually enabled
    private val loadableRuleProviders = mutableSetOf<RuleProvider>()

    // Blocked rule Providers can not be processed because the rule should run after another rule which is not yet added to the new list of
    // rule Providers. As long a no cycle of rule Providers exists which refer to each other, this list will be empty at completion of this
    // function.
    private val blockedRuleProviders = mutableSetOf<RuleProvider>()

    // The list of rule ids which are required to be loaded but are missing. Such rules are referenced in a RunAfterRule visitor modifier
    // and are marked as required to be loaded.
    private val requiredButMissingRuleIds = mutableSetOf<RunAfterRuleRequiredButNotLoaded>()

    override fun filter(ruleProviders: Set<RuleProvider>): Set<RuleProvider> {
        ruleIdsToBeFiltered.addAll(ruleProviders.map { it.ruleId })
        unprocessedRuleProviders.addAll(ruleProviders)

        // Initially the list only contains the rules which have no RunAfterRules (e.g. are not depending on another rule).
        ruleProviders
            .filter { it.hasNoRunAfterRules() }
            .toSet()
            .let {
                loadableRuleProviders.addAll(it)
                unprocessedRuleProviders.removeAll(it)
            }

        var newRuleProvidersAdded: Boolean
        var ruleProvidersIterator: Iterator<RuleProvider> =
            ruleProviders
                .sortedBy {
                    // Ensure that results in logging and unit tests is stable
                    it.ruleId.value
                }.iterator()
        do {
            newRuleProvidersAdded = ruleProvidersIterator.filter()
            if (newRuleProvidersAdded) {
                // All rule Providers which were (previously) blocked can now be checked again in the next iteration
                ruleProvidersIterator =
                    blockedRuleProviders
                        .canRunWith(loadableRuleProviders)
                        .toSet()
                        .iterator()
                blockedRuleProviders.clear()
            }
        } while (newRuleProvidersAdded)
        check(requiredButMissingRuleIds.isEmpty()) { createRequiredRuleIsMissingMessage() }
        check(blockedRuleProviders.isEmpty()) { createCyclicDependencyMessage() }
        check(unprocessedRuleProviders.isEmpty())
        return loadableRuleProviders
    }

    private fun Iterator<RuleProvider>.filter(): Boolean {
        var newRuleProvidersAdded = false
        while (hasNext()) {
            val currentRuleProvider = next()

            when (maxRunAfterRuleOrderModifiers(currentRuleProvider)) {
                ADD, IGNORE -> {
                    // This rule does not depend on any other rule which will be loaded, or it depends on rule for which it already has been
                    // determined that the rule will be loaded before the current rule.
                    loadableRuleProviders.add(currentRuleProvider)
                    unprocessedRuleProviders.remove(currentRuleProvider)
                    newRuleProvidersAdded = true
                }

                BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED -> {
                    // The rule depends on another rule which will be loaded but currently is not yet added to the list of rules which are
                    // loaded before the current rule.
                    blockedRuleProviders.add(currentRuleProvider)
                }

                REQUIRED_RUN_AFTER_RULE_NOT_LOADED -> {
                    // The rule depends on a rule which will not be loaded
                    Unit
                }
            }
        }
        return newRuleProvidersAdded
    }

    private fun maxRunAfterRuleOrderModifiers(ruleProvider: RuleProvider) =
        ruleProvider
            .runAfterRules
            .associateWith { runAfterRule ->
                when {
                    runAfterRule.ruleId in loadableRuleProviders.map { it.ruleId } -> {
                        ADD
                    }

                    runAfterRule.ruleId in unprocessedRuleProviders.map { it.ruleId } -> {
                        BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED
                    }

                    runAfterRule.mode == ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED -> {
                        requiredButMissingRuleIds.add(
                            RunAfterRuleRequiredButNotLoaded(
                                ruleProvider.ruleId,
                                runAfterRule.ruleId,
                            ),
                        )
                        REQUIRED_RUN_AFTER_RULE_NOT_LOADED
                    }

                    else -> {
                        LOGGER.debug {
                            "Rule with id '${ruleProvider.ruleId.value}' should run after the rule with id " +
                                "'${runAfterRule.ruleId.value}'. However, the latter rule is not loaded and is allowed to be ignored. " +
                                "For best results, it is advised load the rule."
                        }
                        IGNORE
                    }
                }
            }.values
            .maxByOrNull { it.severity }
            ?: ADD

    private fun RuleProvider.hasNoRunAfterRules() = runAfterRules.isEmpty()

    private fun Set<RuleProvider>.canRunWith(loadedRuleProviders: Set<RuleProvider>): Set<RuleProvider> =
        canRunWithRuleIds(loadedRuleProviders.map { it.ruleId }.toSet())

    private fun Set<RuleProvider>.canRunWithRuleIds(loadedRuleIds: Set<RuleId>): Set<RuleProvider> =
        this
            .filter { it.canRunWith(loadedRuleIds) }
            .let { unblockedRuleProviders ->
                if (unblockedRuleProviders.isEmpty()) {
                    unblockedRuleProviders
                } else {
                    val unblockedRuleIds = unblockedRuleProviders.map { it.ruleId }
                    this
                        .filter { it.ruleId !in unblockedRuleIds }
                        .toSet()
                        .canRunWithRuleIds(loadedRuleIds.plus(unblockedRuleIds))
                        .plus(unblockedRuleProviders)
                }
            }.toSet()

    private fun RuleProvider.canRunWith(loadedRuleIds: Set<RuleId>): Boolean =
        this
            .runAfterRules
            .all { it.ruleId in loadedRuleIds || it.mode == REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED }

    private fun createRequiredRuleIsMissingMessage(): String {
        val separator = "\n  - "
        return requiredButMissingRuleIds
            .joinToString(
                prefix =
                    "Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to add additional " +
                        "rule sets before creating an issue.$separator",
                separator = separator,
            ) {
                "Rule with id '${it.ruleId}' requires rule with id '${it.runAfterRuleId}' to be loaded"
            }
    }

    private fun createCyclicDependencyMessage(): String {
        val customRuleSetIds =
            blockedRuleProviders
                .map { it.ruleId.ruleSetId }
                .filterNot { it == RuleSetId.STANDARD }
                .map { it.value }
                .distinct()
                .sorted()
        val prefix =
            if (customRuleSetIds.isEmpty()) {
                "Found cyclic dependencies between required rules that should run after another rule:"
            } else {
                "Found cyclic dependencies between required rules that should run after another rule. Please contact " +
                    "the maintainer(s) of the custom rule set(s) [${customRuleSetIds.joinToString()}] before " +
                    "creating an issue in the KtLint project. Dependencies:"
            }
        val separator = "\n  - "
        return blockedRuleProviders.joinToString(prefix = prefix + separator, separator = separator) {
            "Rule with id '${it.ruleId.value}' should run after rule(s) with id '${
                it.runAfterRules.joinToString(separator = ", ") {
                    it.ruleId.value
                }
            }'"
        }
    }

    private data class RunAfterRuleRequiredButNotLoaded(
        val ruleId: RuleId,
        val runAfterRuleId: RuleId,
    )

    private enum class RunAfterRuleOrderModifier(
        val severity: Int,
    ) {
        /**
         *  This rule does not depend on any other rule which will be loaded, or it depends on rule which was already added to the new list
         *  of rule which will be loaded before the current rule.
         */
        ADD(0),

        /**
         * The runAfterRule depends on a rule which will not be loaded but also is not required to be loaded.
         * */
        IGNORE(1),

        /**
         * The runAfterRule depends on a rule which will be loaded but currently is not yet added to the list which is loaded before the
         * current rule.
         */
        BLOCK_UNTIL_RUN_AFTER_RULE_IS_LOADED(2),

        /**
         * The rule depends on a rule which will not be loaded.
         */
        REQUIRED_RUN_AFTER_RULE_NOT_LOADED(3),
    }
}
