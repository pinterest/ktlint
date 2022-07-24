package com.pinterest.ktlint.reporter.format

import java.io.PrintStream
import java.lang.System.out
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class FormatReporterProviderTest {
    @Test
    fun `Given that the format configuration option is not specified then then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                FormatReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = emptyMap()
                    )
            }.withMessage("Format is not specified in config options")
    }

    @Test
    fun `Given that the format configuration and a valid color name is provided then the format reporter provider is created without exception`() {
        val formatReporter = FormatReporterProvider().get(
            out = PrintStream(out, true),
            opt = mapOf(
                "format" to "true",
                "color_name" to "RED"
            )
        )

        assertThat(formatReporter).isNotNull
    }

    @Test
    fun `Given that the format configuration is specified with an invalid value and a valid color name is provided then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                FormatReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf(
                            "format" to "invalid",
                            "color_name" to "RED"
                        )
                    )
            }.withMessage("The string doesn't represent a boolean value: invalid")
    }

    @Test
    fun `Given that the format configuration is provided but the color_name attribute name is not provided then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                FormatReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf("format" to "true")
                    )
            }.withMessage("Invalid color parameter.")
    }

    @Test
    fun `Given that the format configuration is provided and the color_name attribute name is empty then throw an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                FormatReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf(
                            "format" to "true",
                            "color_name" to ""
                        )
                    )
            }.withMessage("Invalid color parameter.")
    }

    @Test
    fun `Given that the format configuration is provided an invalid color name is provided then the format reporter provider throws an IllegalArgumentException`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                FormatReporterProvider()
                    .get(
                        out = PrintStream(out, true),
                        opt = mapOf(
                            "format" to "true",
                            "color_name" to "GARBAGE_INPUT"
                        )
                    )
            }.withMessage("Invalid color parameter.")
    }
}
