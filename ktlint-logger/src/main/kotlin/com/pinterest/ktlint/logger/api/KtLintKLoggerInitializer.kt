package com.pinterest.ktlint.logger.api

import io.github.oshai.kotlinlogging.KLogger

/**
 * Default modifier for the KLogger. It can be set only once via [setDefaultLoggerModifier] but it should be set before the first invocation
 * of [initKtLintKLogger].
 */
private var defaultLoggerModifier: ((KLogger) -> Unit)? = null

/**
 * Set the [defaultLoggerModifier]. Note that it can only be set once. It should be set before the first invocation to [initKtLintKLogger].
 * Also note that it depends on the actual logging framework what capabilities can be set at runtime. The Ktlint CLI uses
 * 'ch.qos.logback:logback-classic' as it allows the log level to be changed at run time. See the 'ktlint-api-consumer' module for an
 * example that uses 'org.slf4j:slf4j-simple' that is configured via a properties file.
 */
public fun KLogger.setDefaultLoggerModifier(loggerModifier: (KLogger) -> Unit): KLogger {
    if (defaultLoggerModifier != null) {
        warn {
            """
            The defaultLoggerModifier has been set before and might already have been applied when initializing
            Loggers. Loggers which will be initialized after resetting the defaultLoggerModifier will be initialized
            with the new value. This might result in unpredictable behavior. Except for in unit tests, it is
            recommended to ensure to call this function only once.
            """.trimIndent()
        }
    }
    defaultLoggerModifier = loggerModifier
    return this
}

/**
 * Initializes the logger with the [defaultLoggerModifier].
 */
public fun KLogger.initKtLintKLogger(): KLogger {
    if (defaultLoggerModifier == null) {
        // Initialize the defaultLoggerModifier on the first invocation of initKtlintLogger when it is not yet set.
        // In this way it can be ensured that all loggers are initialized with the exact same logger modifier.
        defaultLoggerModifier = { _ -> }
    }

    return apply { defaultLoggerModifier?.invoke(this) }
}
