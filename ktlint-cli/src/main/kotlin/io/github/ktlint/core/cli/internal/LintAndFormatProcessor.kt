package io.github.ktlint.core.cli.internal

import io.github.ktlint.core.cli.reporter.baseline.doesNotContain
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError.Status.KOTLIN_PARSE_EXCEPTION
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError.Status.KTLINT_RULE_ENGINE_EXCEPTION
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError.Status.LINT_CAN_BE_AUTOCORRECTED
import io.github.ktlint.core.cli.reporter.core.api.KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED
import io.github.ktlint.core.cli.reporter.core.api.ReporterV2
import io.github.ktlint.core.rule.engine.api.Code
import io.github.ktlint.core.rule.engine.api.KtLintParseException
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.api.KtLintRuleException
import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT
import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision.NO_AUTOCORRECT
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.file.FileSystems
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.text.Charsets.UTF_8

internal class LintAndFormatProcessor(
    private val format: Boolean,
    private val ignoreAutocorrectFailures: Boolean,
    private val forceLintAfterFormat: Boolean,
    private val relative: Boolean,
    private val limit: Int,
) {
    internal val containsUnfixedLintErrors = AtomicBoolean()
    internal val fileNumber = AtomicInteger()
    internal val errorNumber = AtomicInteger()
    internal val adviseToUseFormat = AtomicBoolean()

    internal fun lintFiles(
        patterns: List<String>,
        ktLintRuleEngine: KtLintRuleEngine,
        lintErrorsPerFile: Map<String, List<KtlintCliError>>,
        reporter: ReporterV2,
    ) {
        FileSystems
            .getDefault()
            .fileSequence(patterns)
            .map { it.toFile() }
            .takeWhile { errorNumber.get() < limit }
            .map { file ->
                Callable {
                    file to
                        process(
                            ktLintRuleEngine = ktLintRuleEngine,
                            code = Code.fromFile(file),
                            baselineLintErrors =
                                lintErrorsPerFile
                                    .getOrDefault(
                                        // Baseline stores the lint violations as relative path to work dir
                                        file.location(true),
                                        emptyList(),
                                    ),
                        )
                }
            }.parallel({ (file, errList) -> report(file.location(relative), errList, reporter) })
    }

    private fun process(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        if (code.fileName != null) {
            logger.trace { "Checking ${File(code.fileName!!).location(relative)}" }
        }
        return if (format) {
            formatCode(ktLintRuleEngine, code, baselineLintErrors)
        } else {
            lintCode(ktLintRuleEngine, code, baselineLintErrors)
        }
    }

    private fun formatCode(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        val ktlintCliErrors = mutableListOf<KtlintCliError>()
        try {
            ktLintRuleEngine
                .format(code) { lintError ->
                    if (ignoreAutocorrectFailures && !lintError.canBeAutoCorrected) {
                        NO_AUTOCORRECT
                    } else {
                        KtlintCliError(
                            line = lintError.line,
                            col = lintError.col,
                            ruleId = lintError.ruleId.value,
                            detail =
                                lintError
                                    .detail
                                    .applyIf(!lintError.canBeAutoCorrected) { "$this (cannot be auto-corrected)" },
                            status =
                                if (lintError.canBeAutoCorrected) {
                                    FORMAT_IS_AUTOCORRECTED
                                } else {
                                    LINT_CAN_NOT_BE_AUTOCORRECTED
                                },
                        ).takeIf { baselineLintErrors.doesNotContain(it) }
                            ?.let { ktlintCliError ->
                                ktlintCliErrors.add(ktlintCliError)
                                if (lintError.canBeAutoCorrected) {
                                    ALLOW_AUTOCORRECT
                                } else {
                                    containsUnfixedLintErrors.set(true)
                                    NO_AUTOCORRECT
                                }
                            } ?: NO_AUTOCORRECT
                    }
                }.also { formattedFileContent ->
                    if (ktlintCliErrors.isNotEmpty() && code.content == formattedFileContent) {
                        // In very rare cases it is possible that Lint violations are detected but that they have opposite effects and the
                        // file gets not altered at all. This is to be treated as unfixable error.
                        containsUnfixedLintErrors.set(true)
                    }
                    if (forceLintAfterFormat && code.content != formattedFileContent) {
                        // Rerun lint to check that the formatted code can still be successfully parsed.
                        try {
                            ktLintRuleEngine.lint(Code.fromSnippet(formattedFileContent, code.script))
                        } catch (e: KtLintParseException) {
                            logger.error(e) { "After formatting code in file '${code.filePath}' it cannot be successfully parsed anymore." }
                            // Intentionally stop formatting other files. The '--force-lint-after-format' is only supposed to be used during
                            // release testing of Ktlint CLI. An immediate exit forces to fix the issue immediately as release test can
                            // otherwise not be completed.
                            exitKtLintProcess(ExitCode.PARSE_EXCEPTION_AFTER_FORMAT)
                        }
                    }
                    when {
                        code.isStdIn -> {
                            writeToStdout(formattedFileContent)
                        }

                        code.content != formattedFileContent -> {
                            code
                                .filePath
                                ?.toFile()
                                ?.writeText(formattedFileContent, UTF_8)
                        }
                    }
                }
        } catch (e: KtLintParseException) {
            if (code.isStdIn) {
                if (code.script) {
                    // When reading from stdin, code is only parsed as Kotlin script, if it could not be parsed as pure Kotlin. Now
                    // parsing of the code has failed for both, the file has to be ignored.
                    logger.error {
                        """
                        Can not parse input from <stdin> as Kotlin script, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        """.trimIndent()
                    }
                    // Write original code back to stdout before exiting with a non-zero error code. This is useful in case a tool that
                    // integrates with Ktlint CLI without checking the exit code does not replace the original content with an empty
                    // stdout but just with the original content instead.
                    writeToStdout(code.content)
                    exitKtLintProcess(ExitCode.PARSE_EXCEPTION_STDIN)
                } else {
                    // When reading from stdin, it is first assumed that the provided code is pure Kotlin instead of Kotlin script. If
                    // parsing fails, retry parsing at Kotlin script.
                    logger.warn {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        Now, trying to read the input as Kotlin Script.
                        """.trimIndent()
                    }
                    return formatCode(
                        ktLintRuleEngine = ktLintRuleEngine,
                        code = Code.fromSnippet(code.content, script = true),
                        baselineLintErrors = baselineLintErrors,
                    )
                }
            } else {
                if (!ignoreAutocorrectFailures) {
                    ktlintCliErrors.add(e.toKtlintCliError(code))
                    containsUnfixedLintErrors.set(true)
                }
                code.content // making sure `cat file | ktlint --stdin > file` is (relatively) safe
            }
        } catch (
            // Third party rules can throw any exception or error
            @Suppress("TooGenericExceptionCaught")
            t: Throwable,
        ) {
            if (code.isStdIn) {
                // Write original code back to stdout before exiting with a non-zero error code. This is useful in case a tool that
                // integrates with Ktlint CLI without checking the exit code does not replace the original content with an empty
                // stdout but just with the original content instead.
                writeToStdout(code.content)
                logger.error(t) {}
                exitKtLintProcess(ExitCode.EXCEPTION_STDIN)
            } else {
                if (!ignoreAutocorrectFailures) {
                    ktlintCliErrors.add(t.toKtlintCliError(code))
                    containsUnfixedLintErrors.set(true)
                }
                code.content // making sure `cat file | ktlint --stdin > file` is (relatively) safe
            }
        }
        return ktlintCliErrors.toList()
    }

    private fun writeToStdout(content: String) {
        // Avoid Unicode characters on Windows OS to be malformed as stdout does not default to UTF-8 (#3133).
        with(PrintWriter(OutputStreamWriter(System.out, UTF_8), true)) {
            // Do not use print, print which invoke the format functions which will malfunction when the input file
            // contains a '%' character (#3278).
            write(content)
            // The write method does not autoflush.
            flush()
        }
    }

    private fun Throwable.toKtlintCliError(code: Code): KtlintCliError =
        this.let { e ->
            when (e) {
                is KtLintParseException -> {
                    KtlintCliError(
                        line = e.line,
                        col = e.col,
                        ruleId = "",
                        detail = "Not a valid Kotlin file (${e.message?.lowercase(Locale.getDefault())})",
                        status = KOTLIN_PARSE_EXCEPTION,
                    )
                }

                is KtLintRuleException -> {
                    logger.debug(e) { "Internal Error (${e.ruleId}) in ${code.fileNameOrStdin()} at position '${e.line}:${e.col}" }
                    KtlintCliError(
                        line = e.line,
                        col = e.col,
                        ruleId = "",
                        detail =
                            "Internal Error (rule '${e.ruleId}') in ${code.fileNameOrStdin()} at position '${e.line}:${e.col}. Please " +
                                "create a ticket at https://github.com/ktlint/ktlint/issues and provide the source code that " +
                                "triggered an error.\n" +
                                e.stackTraceToString(),
                        status = KTLINT_RULE_ENGINE_EXCEPTION,
                    )
                }

                else -> {
                    throw e
                }
            }
        }

    private fun lintCode(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        val ktlintCliErrors = mutableListOf<KtlintCliError>()
        try {
            ktLintRuleEngine.lint(code) { lintError ->
                val ktlintCliError =
                    KtlintCliError(
                        line = lintError.line,
                        col = lintError.col,
                        ruleId = lintError.ruleId.value,
                        detail = lintError.detail,
                        status =
                            if (lintError.canBeAutoCorrected) {
                                LINT_CAN_BE_AUTOCORRECTED
                            } else {
                                LINT_CAN_NOT_BE_AUTOCORRECTED
                            },
                    )
                if (baselineLintErrors.doesNotContain(ktlintCliError) && !ignoreAutocorrectFailures) {
                    ktlintCliErrors.add(ktlintCliError)
                    containsUnfixedLintErrors.set(true)
                }
            }
        } catch (e: KtLintParseException) {
            if (code.isStdIn) {
                if (code.script) {
                    logger.error {
                        """
                        Can not parse input from <stdin> as Kotlin script, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        """.trimIndent()
                    }
                    exitKtLintProcess(ExitCode.PARSE_EXCEPTION_STDIN)
                } else {
                    logger.warn {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        Now, trying to read the input as Kotlin Script.
                        """.trimIndent()
                    }
                    return lintCode(
                        ktLintRuleEngine = ktLintRuleEngine,
                        code = Code.fromSnippet(code.content, script = true),
                        baselineLintErrors = baselineLintErrors,
                    )
                }
            } else if (!ignoreAutocorrectFailures) {
                ktlintCliErrors.add(e.toKtlintCliError(code))
                containsUnfixedLintErrors.set(true)
            }
        } catch (
            // Third party rules can throw any exception or error
            @Suppress("TooGenericExceptionCaught")
            t: Throwable,
        ) {
            if (code.isStdIn) {
                logger.error(t) {}
                exitKtLintProcess(ExitCode.EXCEPTION_STDIN)
            } else if (!ignoreAutocorrectFailures) {
                ktlintCliErrors.add(t.toKtlintCliError(code))
                containsUnfixedLintErrors.set(true)
            }
        }
        return ktlintCliErrors.toList()
    }

    private fun report(
        relativeRoute: String,
        ktlintCliErrors: List<KtlintCliError>,
        reporter: ReporterV2,
    ) {
        fileNumber.incrementAndGet()
        val errListLimit = minOf(ktlintCliErrors.size, maxOf(limit - errorNumber.get(), 0))
        errorNumber.addAndGet(errListLimit)
        if (!adviseToUseFormat.get() && ktlintCliErrors.containsErrorThatCanBeAutocorrected()) {
            adviseToUseFormat.set(true)
        }

        reporter.before(relativeRoute)
        ktlintCliErrors
            .take(errListLimit)
            .filterNot { ignoreAutocorrectFailures && it.status == LINT_CAN_NOT_BE_AUTOCORRECTED }
            .forEach { reporter.onLintError(relativeRoute, it) }
        reporter.after(relativeRoute)
    }

    private fun List<KtlintCliError>.containsErrorThatCanBeAutocorrected() = any { it.status == LINT_CAN_BE_AUTOCORRECTED }

    internal fun lintStdin(
        ktLintRuleEngine: KtLintRuleEngine,
        reporter: ReporterV2,
        code: Code,
    ) {
        report(
            KtLintRuleEngine.STDIN_FILE,
            process(
                ktLintRuleEngine = ktLintRuleEngine,
                code = code,
                baselineLintErrors = emptyList(),
            ),
            reporter,
        )
    }
}
