package com.pinterest.ktlint.cli.ruleset.core.api

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.io.Serializable

/**
 * KtLint uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available [RuleSetProviderV3]`s on the classpath and so each [RuleSetProviderV3] must be registered using
 * `META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3` (see `ktlint-ruleset-standard/src/main/resources`
 * for an example).
 */
public abstract class RuleSetProviderV3(
    public val id: RuleSetId,
) : Serializable {
    /**
     * Gets a group of related [RuleProvider]s. A provided rule is not guaranteed to be run as rules can be disabled,
     * for example via ".editorconfig" properties.
     *
     * Intended usage:
     * ```
     * public class CustomRuleSetProvider :
     *     RuleSetProviderV3(RuleSetId("custom")) {
     *     override fun getRuleProviders(): Set<RuleProvider> =
     *         setOf(
     *             RuleProvider { CustomRule1() },
     *             RuleProvider { CustomRule2() }
     *         )
     *     }
     * ```
     */
    public abstract fun getRuleProviders(): Set<RuleProvider>
}
