package io.github.ktlint.core.cli.internal

import kotlin.system.exitProcess

internal fun exitKtLintProcess(exitCode: ExitCode): Nothing {
    logger.debug { "Exit ktlint with exit code: ${exitCode.value}" }
    exitProcess(exitCode.value)
}

@Suppress("MagicNumber")
internal enum class ExitCode(
    val value: Int,
) {
    // Do not change the values of the ExitCode as they might be used by external integrations with Ktlint CLI
    OK(0),
    HAS_UNFIXED_LINT_ERRORS_AFTER_FORMAT(1),
    IO_EXCEPTION(2),
    PARSE_EXCEPTION_STDIN(3),
    EXCEPTION_STDIN(4),
    FILE_NOT_FOUND(5),
    INVALID_RULESET_JAR(6),
    INVALID_REPORTER_CONFIGURATION(7),
    PARSE_EXCEPTION_AFTER_FORMAT(123),
}
