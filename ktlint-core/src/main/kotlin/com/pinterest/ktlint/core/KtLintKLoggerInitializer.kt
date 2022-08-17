package com.pinterest.ktlint.core

import mu.KLogger

/**
 * Default modifier for the KLogger. It can be set only once via [setDefaultLoggerModifier] but it should be set before
 * the first invocation of [initKtLintKLogger].
 */
private var defaultLoggerModifier: ((KLogger) -> Unit)? = null

/**
 * Set the [defaultLoggerModifier]. Note that it can only be set once. It should be set before the first invocation to
 * [initKtLintKLogger].
 */
public fun KLogger.setDefaultLoggerModifier(
    loggerModifier: (KLogger) -> Unit,
): KLogger {
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
