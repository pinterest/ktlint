package com.pinterest.ktlint.core

import mu.KLogger

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
private const val KTLINT_DEBUG = "KTLINT_DEBUG"

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
public const val KTLINT_UNIT_TEST_DUMP_AST = "KTLINT_UNIT_TEST_DUMP_AST"

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
public const val KTLINT_UNIT_TEST_ON_PROPERTY = "ON"

/**
 * Default modifier for the KLogger of new instances of classes calling [initKtLintKLogger]. Classes for which
 * [initKtLintKLogger] has been called before setting this variable will not be changed. Also note, that this modifier
 * can only be set once.
 */
public lateinit var defaultLoggerModifier: (KLogger) -> Unit

/**
 * Initializes the logger with the [defaultLoggerModifier] when set.
 */
public fun KLogger.initKtLintKLogger(): KLogger {
    return if (::defaultLoggerModifier.isInitialized) {
        apply { defaultLoggerModifier(this) }
    } else {
        this
    }
}

/**
 * Initializes the logger with the [loggerModifier].
 */
public fun KLogger.initKtLintKLogger(
    loggerModifier: (KLogger) -> Unit
): KLogger = apply { loggerModifier(this) }
