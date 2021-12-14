package com.pinterest.ktlint

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.security.Permission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaselineTests {

    @Before
    fun setup() {
        System.setSecurityManager(object : SecurityManager() {
            override fun checkPermission(perm: Permission?) { // allow anything.
            }

            override fun checkPermission(perm: Permission?, context: Any?) { // allow anything.
            }

            override fun checkExit(status: Int) {
                super.checkExit(status)
                throw ExitException(status)
            }
        })
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
        assertTrue(output.contains(".*:1:24: Unnecessary block".toRegex()))
        assertTrue(output.contains(".*:2:1: Unexpected blank line\\(s\\) before \"}\"".toRegex()))
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
        assertFalse(output.contains(".*:1:24: Unnecessary block".toRegex()))
        assertFalse(output.contains(".*:2:1: Unexpected blank line\\(s\\) before \"}\"".toRegex()))
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
        assertFalse(output.contains(".*:1:24: Unnecessary block".toRegex()))
        assertTrue(output.contains(".*:2:1: Unexpected blank line\\(s\\) before \"}\"".toRegex()))
    }

    private class ExitException(val status: Int) : SecurityException("Should not exit in tests")
}
