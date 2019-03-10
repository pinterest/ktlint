package com.github.shyiko.ktlint.internal

import org.assertj.core.api.Assertions
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

class ReporterLoaderKtTest {

    private val debug = true
    private val dependencyResolver = MavenDependencyResolver.lazyResolver(emptyList(), false, debug)

    @AfterMethod
    fun enableSystemExit() = enableSystemExitCall()

    @Test(expectedExceptions = [SystemExitException::class])
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
