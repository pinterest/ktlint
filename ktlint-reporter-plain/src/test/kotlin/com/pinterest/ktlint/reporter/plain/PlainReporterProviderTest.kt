package com.pinterest.ktlint.reporter.plain

import com.pinterest.ktlint.reporter.plain.internal.Color
import java.io.PrintStream
import java.lang.System.out
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainReporterProviderTest {
    @Test
    fun testColorDefaults() {
        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf()
        ) as PlainReporter

        assertFalse(plainReporter.shouldColorOutput)
        assertEquals(Color.DARK_GRAY, plainReporter.outputColor)
    }

    @Test
    fun testValidShouldColorBoolean() {
        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf("color" to "true")
        ) as PlainReporter

        assertTrue(plainReporter.shouldColorOutput)
    }

    @Test
    fun testInvalidOutputColor() {
        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf("outputColor" to "GarbageInput")
        ) as PlainReporter

        assertEquals(Color.DARK_GRAY, plainReporter.outputColor)
    }

    @Test
    fun testValidOutputColor() {
        val outputColor = Color.RED

        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf("outputColor" to outputColor.name)
        ) as PlainReporter

        assertEquals(outputColor, plainReporter.outputColor)
    }
}
