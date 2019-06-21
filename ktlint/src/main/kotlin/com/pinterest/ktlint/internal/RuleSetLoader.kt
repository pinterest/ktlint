package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceLoader

fun getRuleSetProviders(
    dependencyResolver: Lazy<MavenDependencyResolver>,
    ruleSetsUrls: Collection<String>,
    debug: Boolean,
    experimental: Boolean,
    skipClasspathCheck: Boolean
): List<RuleSetProvider> {
    val additionalArtifactUrls: Collection<URL> = if (ruleSetsUrls.isEmpty()) {
        emptyList()
    } else {
        dependencyResolver.value.resolveArtifacts(
            ruleSetsUrls,
            debug,
            skipClasspathCheck
        )
    }

    return loadRuleSetProvider(additionalArtifactUrls, debug = debug, experimental = experimental)
}

private fun loadRuleSetProvider(urls: Collection<URL>, debug: Boolean, experimental: Boolean): List<RuleSetProvider> {
    val rulesetClassLoader = URLClassLoader(urls.toTypedArray(), RuleSet::class.java.classLoader)

    // standard should go first
    val ruleSetProviders = ServiceLoader.load(RuleSetProvider::class.java, rulesetClassLoader)
        .map { it.get().id to it }
        .filter { (id) -> experimental || id != "experimental" }
        .sortedBy { if (it.first == "standard") "\u0000${it.first}" else it.first }
    if (debug) {
        ruleSetProviders.forEach { System.err.println("[DEBUG] Discovered ruleset \"${it.first}\"") }
    }
    return ruleSetProviders.map { it.second }
}
