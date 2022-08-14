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
    fun testNoBaseline() {
        val stream = ByteArrayOutputStream()
        val ps = PrintStream(stream)
        System.setOut(ps)

        try {
            main(arrayOf("src/test/resources/TestBaselineFile.kt"))
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
            main(arrayOf("src/test/resources/TestBaselineFile.kt", "--baseline=src/test/resources/test-baseline.xml"))
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
            main(arrayOf("src/test/resources/TestBaselineExtraErrorFile.kt", "--baseline=src/test/resources/test-baseline.xml"))
        } catch (e: ExitException) {
            // handle System.exit
        }

        val output = String(stream.toByteArray())
        assertThat(output).doesNotContain(":1:34: Unnecessary block")
        assertThat(output).contains(":2:1: Unexpected blank line(s) before \"}\"")
    }

    private class ExitException : SecurityException("Should not exit in tests")
}
