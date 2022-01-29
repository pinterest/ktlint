package com.pinterest.ktlint.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import mu.KLogger
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

public enum class LogLevel { TRACE, DEBUG, INFO }

public var logLevel: LogLevel? = null

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

public fun KLogger.initKtLintKLogger(): KLogger =
    also { logger ->
        System
            .getenv(KTLINT_UNIT_TEST_TRACE)
            .orEmpty()
            .equals(KTLINT_UNIT_TEST_ON_PROPERTY, ignoreCase = true)
            .ifTrue {
                // The log level of the kotlin-logging framework can only be altered by modifying the underling logging
                // library. Also note that the default SLF4J implementation does not allow the log level to be changes.
                // Therefore, a fall back on the logback-core is required. See
                // https://github.com/MicroUtils/kotlin-logging/issues/20
                logger.trace { "Enable TRACE logging as System environment variable $KTLINT_UNIT_TEST_TRACE is set to 'on'" }
                logLevel = LogLevel.TRACE
            }
        if (logLevel == LogLevel.TRACE) {
            (logger.underlyingLogger as Logger).level = Level.TRACE
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
