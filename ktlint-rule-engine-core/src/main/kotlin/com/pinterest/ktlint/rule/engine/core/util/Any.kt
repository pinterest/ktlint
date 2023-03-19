package com.pinterest.ktlint.rule.engine.core.util

/**
 * Safe cast, copied from
 * https://github.com/JetBrains/kotlin/blob/d3200b2c65b829b85244c4ec4cb19f6e479b06ba/core/util.runtime/src/org/jetbrains/kotlin/utils/addToStdlib.kt#L108
 */
public inline fun <reified T : Any> Any?.safeAs(): T? = this as? T

/**
 * Unsafe cast, copied from
 * https://github.com/JetBrains/kotlin/blob/d3200b2c65b829b85244c4ec4cb19f6e479b06ba/core/util.runtime/src/org/jetbrains/kotlin/utils/addToStdlib.kt#L111
 */
public inline fun <reified T : Any> Any?.cast(): T = this as T
