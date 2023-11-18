package com.pinterest.ktlint.cli.reporter.baseline

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.BASELINE_IGNORED
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.xml.sax.SAXParseException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.pathString

@Suppress("unused")
private val LOGGER =
    KotlinLogging
        .logger {}
        .setDefaultLoggerModifier { logger -> logger.level = Level.DEBUG }
        .initKtLintKLogger()

private var KLogger.level: Level?
    get() = underlyingLogger()?.level
    set(value) {
        underlyingLogger()?.level = value
    }

private fun KLogger.underlyingLogger(): Logger? =
    @Suppress("UNCHECKED_CAST")
    (this as? DelegatingKLogger<Logger>)
        ?.underlyingLogger

class BaselineTest {
    @Nested
    inner class `Given a valid baseline file` {
        @Test
        fun `Given that the baseline is loaded with classic loader`(
            @TempDir
            tempDir: Path,
        ) {
            val path = "baseline-valid.xml".copyResourceToFileIn(tempDir)

            val actual = loadBaseline(path)

            assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(
                    Baseline(
                        path = path,
                        status = Baseline.Status.VALID,
                        lintErrorsPerFile =
                            mapOf(
                                "src/main/kotlin/Foo.kt" to
                                    listOf(
                                        KtlintCliError(1, 1, "standard:max-line-length", "", BASELINE_IGNORED),
                                        KtlintCliError(2, 1, "standard:max-line-length", "", BASELINE_IGNORED),
                                        KtlintCliError(4, 9, "standard:property-naming", "", BASELINE_IGNORED),
                                    ),
                            ),
                    ),
                )
        }

        @ParameterizedTest(name = "BaselineErrorHandling: {0}")
        @EnumSource(value = BaselineErrorHandling::class)
        fun `Given that the baseline is loaded with the new loader with LOG error handling`(
            baselineErrorHandling: BaselineErrorHandling,
            @TempDir
            tempDir: Path,
        ) {
            val path = "baseline-valid.xml".copyResourceToFileIn(tempDir)

            val actual = loadBaseline(path, baselineErrorHandling)

            assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(
                    Baseline(
                        path = path,
                        status = Baseline.Status.VALID,
                        lintErrorsPerFile =
                            mapOf(
                                "src/main/kotlin/Foo.kt" to
                                    listOf(
                                        KtlintCliError(1, 1, "standard:max-line-length", "", BASELINE_IGNORED),
                                        KtlintCliError(2, 1, "standard:max-line-length", "", BASELINE_IGNORED),
                                        KtlintCliError(4, 9, "standard:property-naming", "", BASELINE_IGNORED),
                                    ),
                            ),
                    ),
                )
        }
    }

    @Nested
    inner class `Baseline has invalid xml structure` {
        @Test
        fun `Given that the baseline is loaded with classic loader then the log contains an error message`(
            @TempDir
            tempDir: Path,
        ) {
            val path = "baseline-invalid.xml".copyResourceToFileIn(tempDir)

            val logCaptor = LogCaptor.forClass(Baseline::class.java)

            loadBaseline(path)

            assertThat(logCaptor.errorLogs).contains("Unable to parse baseline file: $path")
        }

        @Test
        fun `Given that the baseline is loaded with new loader then an exception is thrown on error`(
            @TempDir
            tempDir: Path,
        ) {
            val path = "baseline-invalid.xml".copyResourceToFileIn(tempDir)

            assertThatExceptionOfType(BaselineLoaderException::class.java)
                .isThrownBy { loadBaseline(path, BaselineErrorHandling.EXCEPTION) }
                .withMessage("Unable to parse baseline file: $path")
                .withCauseInstanceOf(SAXParseException::class.java)
                .havingCause()
                .withMessage("The element type \"file\" must be terminated by the matching end-tag \"</file>\".")
        }

        @Test
        fun `Given that the baseline is loaded with new loader then the log message is printed and no exception is thrown`(
            @TempDir
            tempDir: Path,
        ) {
            val path = "baseline-invalid.xml".copyResourceToFileIn(tempDir)

            val logCaptor = LogCaptor.forClass(Baseline::class.java)

            loadBaseline(path)

            assertThat(logCaptor.errorLogs).contains("Unable to parse baseline file: $path")
        }
    }

    /**
     * The resource needs to be copied in each test, because the file will be deleted in case it contains an error.
     */
    private fun String.copyResourceToFileIn(tempDir: Path) =
        Paths
            .get("$TEST_RESOURCE_PATH/$this")
            .copyTo(tempDir, overwrite = true)
            .pathString

    private companion object {
        val TEST_RESOURCE_PATH: Path = Path("src", "test", "resources")
    }
}
