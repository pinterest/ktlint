package com.pinterest.ktlint.core

import mu.KLogger

@Deprecated("Environment variable is replace by new variables below")
private const val KTLINT_DEBUG = "KTLINT_DEBUG"

public var loggerModifier: (KLogger) -> Unit = { _ -> }

public fun KLogger.initKtLintKLogger(): KLogger =
    also { logger -> loggerModifier(logger) }
