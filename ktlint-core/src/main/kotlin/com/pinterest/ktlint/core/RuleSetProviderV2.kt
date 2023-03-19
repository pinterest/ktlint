package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicyLegacy
import java.io.Serializable

/**
 * `ktlint` uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available `RuleSetProvider`s on the classpath and so each `RuleSetProvider` must be registered using
 * `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider`
 * (see `ktlint-ruleset-standard/src/main/resources` for an example).
 */
@Deprecated("Deprecated since ktlint 0.49.0. Custom rulesets have to be migrated to RuleSetProviderV3. See changelog 0.49.")
public abstract class RuleSetProviderV2(
    public val id: String,
    public val about: About,
) : Serializable {
    init {
        IdNamingPolicyLegacy.enforceRuleSetIdNaming(id)
    }

    /**
     * Gets a group of related [RuleProvider]s. A provided rule is not guaranteed to be run as rules can be disabled,
     * for example via ".editorconfig" properties.
     *
     * Intended usage:
     * ```
     * public class CustomRuleSetProvider :
     *     RuleSetProviderV2(
     *         id = "custom",
     *         about = About(...)
     *     ) {
     *     override fun getRuleProviders(): Set<RuleProvider> =
     *         setOf(
     *             RuleProvider { CustomRule1() },
     *             RuleProvider { CustomRule2() }
     *         )
     *     }
     * ```
     */
    public abstract fun getRuleProviders(): Set<RuleProvider>

    /**
     * For publicly available rule sets, it is advised to provide all details below, so that users of your rule set can
     * easily get up-to-date information about the rule set.
     */
    public data class About(
        /**
         * Name of person, organisation or group maintaining the rule set.
         */
        val maintainer: String?,
        /**
         * Short description of the rule set.
         */
        val description: String?,
        val license: String?,
        val repositoryUrl: String?,
        val issueTrackerUrl: String?,
    ) {
        init {
            require(maintainer == null || maintainer.length <= 50) {
                "Length of maintainer should be 50 characters or less"
            }
            require(description == null || description.length <= 400) {
                "Length of description should be 400 characters or less"
            }
            require(license == null || license.length <= 120) {
                "Length of license url should be 80 characters or less"
            }
            require(repositoryUrl == null || repositoryUrl.length <= 120) {
                "Length of repository url should be 80 characters or less"
            }
            require(issueTrackerUrl == null || issueTrackerUrl.length <= 120) {
                "Length of repository url should be 80 characters or less"
            }
        }
    }

    public companion object {
        public val NO_ABOUT: About =
            About(
                maintainer = "Not specified",
                description = "Not specified",
                license = "Not specified",
                repositoryUrl = "Not specified",
                issueTrackerUrl = "Not specified",
            )
    }
}
