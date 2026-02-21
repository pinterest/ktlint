package com.pinterest.ktlint.cli.ruleset.core.api

import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.RuleV1InstanceProvider
import java.io.Serializable

/**
 * KtLint uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available [RuleSetV1Provider]`s on the classpath and so each [RuleSetV1Provider] must be registered using
 * `META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV1Provider` (see `ktlint-ruleset-standard/src/main/resources`
 * for an example).
 */
public abstract class RuleSetV1Provider(
    public val id: RuleSetId,
) : Serializable {
    /**
     * Gets a group of related [RuleV1InstanceProvider]s. A provided rule is not guaranteed to be run as rules can be disabled,
     * for example via ".editorconfig" properties.
     *
     * Intended usage:
     * ```
     * public class CustomRuleSetProvider :
     *     RuleSetV1Provider(RuleSetId("custom")) {
     *     override fun getRuleProviders(): Set<RuleProvider> =
     *         setOf(
     *             RuleProvider { CustomRule1() },
     *             RuleProvider { CustomRule2() }
     *         )
     *     }
     * ```
     */
    public abstract fun getRuleProviders(): Set<RuleV1InstanceProvider>
}
