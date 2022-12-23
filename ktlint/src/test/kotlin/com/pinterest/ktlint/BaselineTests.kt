package com.pinterest.ktlint

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.security.Permission
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BaselineTests {
    @BeforeEach
    internal fun setUp() {
        System.setSecurityManager(
            object : SecurityManager() {
                override fun checkPermission(perm: Permission?) { // allow anything.
                }

                override fun checkPermission(perm: Permission?, context: Any?) { // allow anything.
                }

                override fun checkExit(status: Int) {
                    super.checkExit(status)
                    throw ExitException()
                }
            },
        )
    }

    @Test
    fun `Given a file containing lint errors then find those errors`() {
        val stream = ByteArrayOutputStream()
        val ps = PrintStream(stream)
        System.setOut(ps)

        try {
            main(arrayOf("src/test/resources/cli/baseline/TestBaselineFile.kt.test"))
        } catch (e: ExitException) {
            // handle System.exit
        }

        val output = String(stream.toByteArray())
        assertThat(output).contains(":1:24: Unnecessary block")
        assertThat(output).contains(":2:1: Unexpected blank line(s) before \"}\"")
    }

    @Test
    fun testBaselineReturnsNoErrors() {
        val stream = ByteArrayOutputStream()
        val ps = PrintStream(stream)
        System.setOut(ps)

        try {
            main(arrayOf("src/test/resources/cli/baseline/TestBaselineFile.kt.test", "--log-level=debug", "--baseline=src/test/resources/cli/baseline/test-baseline.xml"))
        } catch (e: ExitException) {
            // handle System.exit
        }

        val output = String(stream.toByteArray())
        assertThat(output).doesNotContain(":1:24: Unnecessary block")
        assertThat(output).doesNotContain(":2:1: Unexpected blank line(s) before \"}\"")
    }

    @Test
    fun testExtraErrorNotInBaseline() {
        val stream = ByteArrayOutputStream()
        val ps = PrintStream(stream)
        System.setOut(ps)

        try {
            main(arrayOf("src/test/resources/cli/baseline/TestBaselineExtraErrorFile.kt.test", "--baseline=src/test/resources/cli/baseline/test-baseline.xml", "-l=debug"))
        } catch (e: ExitException) {
            // handle System.exit
        }

        val output = String(stream.toByteArray())
        assertThat(output).doesNotContain(":1:34: Unnecessary block")
        assertThat(output).contains(":2:1: Unexpected blank line(s) before \"}\"")
    }

    private class ExitException : SecurityException("Should not exit in tests")
}
