package com.pinterest.ktlint.internal

import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Test

class ReporterLoaderTest {

    private val debug = true
    private val dependencyResolver = MavenDependencyResolver.lazyResolver(emptyList(), false, debug)

    @After
    fun enableSystemExit() = enableSystemExitCall()

    @Test(expected = SystemExitException::class)
    fun testFailWithUnknownReporter() {
        disableSystemExitCall()

        val reporter = loadReporter(
            dependencyResolver,
            reporters = listOf("foo"),
            debug = debug,
            verbose = true,
            stdin = true,
            color = true,
            skipClasspathCheck = false
        ) { true }

        Assertions.assertThat(reporter).isNull()
    }

    @Test
    fun testFallbackToPlainReporter() {
        val reporter = loadReporter(
            dependencyResolver,
            reporters = emptyList(),
            debug = debug,
            verbose = true,
            stdin = true,
            color = true,
            skipClasspathCheck = false
        ) { true }

        Assertions.assertThat(reporter).isNotNull()
    }

    @Test
    fun testLoadPlainReporter() {
        val reporter = loadReporter(
            dependencyResolver,
            reporters = listOf("plain"),
            debug = debug,
            verbose = true,
            stdin = true,
            color = true,
            skipClasspathCheck = false
        ) { true }

        Assertions.assertThat(reporter).isNotNull()
    }

    @Test
    fun testLoadJsonReporter() {
        val reporter = loadReporter(
            dependencyResolver,
            reporters = listOf("json"),
            debug = debug,
            verbose = true,
            stdin = true,
            color = true,
            skipClasspathCheck = false
        ) { true }

        Assertions.assertThat(reporter).isNotNull()
    }
}
