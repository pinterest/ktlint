package com.github.shyiko.ktlint.internal

import com.github.shyiko.ktlint.core.RuleSet
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class RuleSetLoaderKtTest {

    private val debug = true
    private val dependencyResolver = MavenDependencyResolver.lazyResolver(emptyList(), false, debug)

    @Test
    fun testLoadingExtraRuleSetsFromLocalJar() {
        val artifacts = listOf("src/test/resources/custom.jar")

        val ruleSets = loadRuleSets(
            dependencyResolver,
            ruleSetsUrls = artifacts,
            debug = debug,
            experimental = true,
            skipClasspathCheck = false
        )

        assertThat(ruleSets.map(RuleSet::id)).contains("custom")
    }

    @Test
    fun testLoadingExtraRuleSetsFromMavenRepo() {
        val artifacts = listOf("com.gabrielittner.ktlint:ktlint-rules:0.2.0")
        val ruleSets = loadRuleSets(
            dependencyResolver,
            ruleSetsUrls = artifacts,
            debug = debug,
            experimental = true,
            skipClasspathCheck = false
        )

        assertThat(ruleSets.map(RuleSet::id)).contains("custom")
    }

    @Test
    fun testExcludeExperimentaRuleSet() {
        val ruleSets = loadRuleSets(
            dependencyResolver,
            ruleSetsUrls = emptyList(),
            debug = debug,
            experimental = false,
            skipClasspathCheck = false
        )

        assertThat(ruleSets.map(RuleSet::id)).containsExactly("standard")
    }

    @Test
    fun testIncludeExperimentaRuleSetStandardFirst() {
        val ruleSets = loadRuleSets(
            dependencyResolver,
            ruleSetsUrls = emptyList(),
            debug = debug,
            experimental = true,
            skipClasspathCheck = false
        )

        assertThat(ruleSets.map(RuleSet::id)).containsExactly("standard", "experimental")
    }
}
