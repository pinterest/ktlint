package com.pinterest.ktlint.core

import java.io.Serializable

/**
 * `ktlint` uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available `RuleSetProvider`s on the classpath and so each `RuleSetProvider` must be registered using
 * `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider`
 * (see `ktlint-ruleset-standard/src/main/resources` for an example).
 *
 * For backward compatability, it is advised that custom rule set providers do *not* yet remove the implementation of
 * the [RuleSetProvider] yet, but to implement the [RuleSetProviderV2] and [RuleSetProvider] on the same class. In this
 * way, the same rule set JAR file can be used with old (0.46.x and before) and new (0.47.x and beyond) versions of
 * KtLint.
 */
@Deprecated("Marked for removal in KtLint 0.48. See changelog or KDoc for more information.")
public interface RuleSetProvider : Serializable {
    /**
     * This method is going to be called once for each file (which means if any of the rules have state or
     * are not thread-safe - a new RuleSet must be created).
     *
     * This method is marked for removal in KtLint 0.48 for reason below:
     *
     * For each invocation of [KtLint.lint] and [KtLint.format] the [RuleSet] is retrieved. This results in new
     * instances of each [Rule] for each file being processed. As of that a [Rule] does not need to be thread-safe.
     *
     * However, [KtLint.format] requires the [Rule] to be executed twice on o a file in case at least one violation
     * has been autocorrected. As the same [Rule] instance is reused for the second execution of the [Rule], the state
     * of the [Rule] is shared. As of this [Rule] have to clear their internal state.
     */
    @Deprecated(
        "Marked for removal in KtLint 0.48. See changelog or KDoc for more information.",
    )
    public fun get(): RuleSet
}
