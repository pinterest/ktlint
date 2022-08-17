package com.pinterest.ktlint.reporter.plain

import java.io.PrintStream
import java.lang.System.out
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class PlainReporterProviderTest {
    @Test
    fun `Given that a valid color name is provided then the plain reporter provider is created without exception`() {
        val plainReporter = PlainReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf("color_name" to "RED"),
        )

        assertThat(plainReporter).isNotNull
    }

    @Test
    fun `Given that the color_name attribute name is not provided then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                PlainReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf(),
                    )
            }.withMessage("Invalid color parameter.")
    }

    @Test
    fun `Given that the color_name attribute name is empty then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                PlainReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf("color_name" to ""),
                    )
            }.withMessage("Invalid color parameter.")
    }

    @Test
    fun `Given that an invalid color name is provided then the plain reporter provider throws an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                PlainReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf("color_name" to "GARBAGE_INPUT"),
                    )
            }.withMessage("Invalid color parameter.")
    }
}
