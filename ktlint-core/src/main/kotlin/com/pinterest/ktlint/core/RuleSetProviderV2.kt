package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy
import java.io.Serializable

/**
 * `ktlint` uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available `RuleSetProvider`s on the classpath and so each `RuleSetProvider` must be registered using
 * `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider`
 * (see `ktlint-ruleset-standard/src/main/resources` for an example).
 *
 * In a future version of KtLint, the [RuleSetProvider] will be removed. For backward compatability, it is advised that
 * custom rule set providers implement the [RuleSetProviderV2] and [RuleSetProvider] on the same class. In this way, the
 * same rule set JAR file can be used with old (0.46.x and before) and new (0.47.x and beyond) versions of
 * KtLint.
 */

public abstract class RuleSetProviderV2(
    public val id: String,
    public val about: About
) : Serializable {

    init {
        IdNamingPolicy.enforceRuleSetIdNaming(id)
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
        val issueTrackerUrl: String?
    ) {
        init {
            require(maintainer == null || maintainer.length <= 50) {
                "Length of maintainer should be 50 characters or less"
            }
            require(description == null || description.length <= 400) {
                "Length of description should be 400 characters or less"
            }
            require(license == null || license.length <= 80) {
                "Length of license should be 80 characters or less"
            }
            require(repositoryUrl == null || repositoryUrl.length <= 80) {
                "Length of repository url should be 80 characters or less"
            }
            require(issueTrackerUrl == null || issueTrackerUrl.length <= 80) {
                "Length of repository url should be 80 characters or less"
            }
        }
    }

    public companion object {
        public val NO_ABOUT = About(
            maintainer = "Not specified",
            description = "Not specified",
            license = "Not specified",
            repositoryUrl = "Not specified",
            issueTrackerUrl = "Not specified"
        )
    }
}
