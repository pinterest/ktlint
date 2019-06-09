package com.pinterest.ktlint.core

/**
 * `ktlint` uses [ServiceLoader](http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available `RuleSetProvider`s on the classpath and so each `RuleSetProvider` must be registered using
 * `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider`
 * (see `ktlint-ruleset-standard/src/main/resources` for an example).
 */
interface RuleSetProvider {

    /**
     * This method is going to be called once for each file (which means if any of the rules have state or
     * are not thread-safe - a new RuleSet must be created).
     */
    fun get(): RuleSet
}
