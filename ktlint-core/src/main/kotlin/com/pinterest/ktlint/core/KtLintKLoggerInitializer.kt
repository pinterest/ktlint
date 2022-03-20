package com.pinterest.ktlint.core

import mu.KLogger

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
private const val KTLINT_DEBUG = "KTLINT_DEBUG"

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
public const val KTLINT_UNIT_TEST_DUMP_AST = "KTLINT_UNIT_TEST_DUMP_AST"

@Deprecated(message = "No longer in use for the public API. Constant is marked for removal in Ktlint 0.46.")
public const val KTLINT_UNIT_TEST_ON_PROPERTY = "ON"

/**
 * Initializes the logger. Optionally the logger can be modified using the [loggerModifier].
 */
public fun KLogger.initKtLintKLogger(
    loggerModifier: (KLogger) -> Unit = { _ -> }
): KLogger = apply { loggerModifier(this) }
