package com.pinterest.ktlint.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import mu.KLogger

public enum class LogLevel { TRACE, DEBUG, INFO }

public var logLevel: LogLevel = LogLevel.INFO

@Deprecated("Environment variable is replace by new variables below")
private const val KTLINT_DEBUG = "KTLINT_DEBUG"

// Via command line parameters "--trace" and "--print-ast" the end user of ktlint can change the logging behavior. As
// unit tests are not invoked via the main ktlint runtime, those command line parameters can not be used to change the
// logging behavior while running the unit tests. Instead, the environment variables below can be used by ktlint
// developers to change the logging behavior. Note, when set the environment variables also affect the runtinme of
// ktlint. As of that the name of the variables start with KTLINT_UNIT_TEST to clarify the intent.
public const val KTLINT_UNIT_TEST_DUMP_AST = "KTLINT_UNIT_TEST_DUMP_AST"
public const val KTLINT_UNIT_TEST_TRACE = "KTLINT_UNIT_TEST_TRACE"
public const val KTLINT_UNIT_TEST_ON_PROPERTY = "ON"

private var isLogLevelCheckedBefore = true

public fun KLogger.initKtLintKLogger(): KLogger =
    also { logger ->
        val enableUnitTestTracing =
            System
                .getenv(KTLINT_UNIT_TEST_TRACE)
                .orEmpty()
                .equals(KTLINT_UNIT_TEST_ON_PROPERTY, ignoreCase = true)

        if (logger.underlyingLogger is Logger) {
            setLogbackLoggerLevel(logger, enableUnitTestTracing)
        } else {
            checkExpectedLogLevelNonLogbackLogger(logger)
        }

        System
            .getenv(KTLINT_DEBUG)
            .orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.let {
                logger.error {
                    """
                    System environment variable $KTLINT_DEBUG is no longer used to change the logging behavior while running unit tests.
                    Now set one or more of environment variables below:
                        $KTLINT_UNIT_TEST_TRACE=[on|off]
                        $KTLINT_UNIT_TEST_DUMP_AST=[on|off]
                    """.trimIndent()
                }
            }
    }

private fun setLogbackLoggerLevel(logger: KLogger, enableUnitTestTracing: Boolean) {
    require(logger.underlyingLogger is Logger)
    // The log level of the kotlin-logging framework can only be altered by modifying the underling logging
    // library in case that logger is a logback-core logger as the default SLF4J implementation does not allow
    // the log level to be changed See https://github.com/MicroUtils/kotlin-logging/issues/20
    (logger.underlyingLogger as Logger).level = when {
        logLevel == LogLevel.TRACE || enableUnitTestTracing -> Level.TRACE
        logLevel == LogLevel.DEBUG -> Level.DEBUG
        else -> Level.INFO
    }

    if (enableUnitTestTracing) {
        logger.trace { "Enabled TRACE logging as System environment variable $KTLINT_UNIT_TEST_TRACE is set to 'on'" }
    }
}

private fun checkExpectedLogLevelNonLogbackLogger(logger: KLogger) {
    require(logger.underlyingLogger !is Logger)
    if (isLogLevelCheckedBefore) {
        return
    }

    if ((logLevel == LogLevel.TRACE && !logger.isTraceEnabled) ||
        (logLevel == LogLevel.DEBUG && !logger.isDebugEnabled)
    ) {
        logger.error {
            """
            The logLevel can not be changed to ${logLevel.name} as the the provided Logger does not allow this.
            Either change the loglevel of the provided logger before calling Ktlint or replace the provided
            logger with a Logback Logger.
            """.trimIndent()
        }
        // Prevent printing that the log message is printed for every single class that initializes its logger
        isLogLevelCheckedBefore = false
    }
}
