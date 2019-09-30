package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.reporter.plain.internal.Color
import java.io.PrintStream
import java.lang.System.out
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PlainReporterProviderTest {
    @Test
    fun testNoColorNameProvided() {
        try {
            PlainReporterProvider().get(
                out = PrintStream(out, true),
                opt = mapOf()
            ) as PlainReporter

            fail("Expected IllegalArgumentException.")
        } catch (iae: IllegalArgumentException) {
            // Expected case
        } catch (e: Exception) {
            fail("Expected IllegalArgumentException but was: $e")
        }
    }

    @Test
    fun testEmptyColorNameProvided() {
        try {
            PlainReporterProvider().get(
                out = PrintStream(out, true),
                opt = mapOf("color_name" to "")
            ) as PlainReporter

            fail("Expected IllegalArgumentException.")
        } catch (iae: IllegalArgumentException) {
            // Expected case
        } catch (e: Exception) {
            fail("Expected IllegalArgumentException but was: $e")
        }
    }

    @Test
    fun testValidColorNameProvided() {
        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf("color_name" to "RED")
        ) as PlainReporter

        assertEquals(Color.RED, plainReporter.outputColor)
    }

    @Test
    fun testInvalidColorNameProvided() {
        try {
            PlainReporterProvider().get(
                out = PrintStream(out, true),
                opt = mapOf("colo_namer" to "GARBAGE_INPUT")
            ) as PlainReporter

            fail("Expected IllegalArgumentException.")
        } catch (iae: IllegalArgumentException) {
            // Expected case
        } catch (e: Exception) {
            fail("Expected IllegalArgumentException but was: $e")
        }
    }
}
