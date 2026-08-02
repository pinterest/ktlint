package com.pinterest.ktlint.rule.engine.core.util

/**
 * Safe cast, copied from
 * https://github.com/JetBrains/kotlin/blob/d3200b2c65b829b85244c4ec4cb19f6e479b06ba/core/util.runtime/src/org/jetbrains/kotlin/utils/addToStdlib.kt#L108
 */
@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public inline fun <reified T : Any> Any?.safeAs(): T? = this as? T

/**
 * Unsafe cast, copied from
 * https://github.com/JetBrains/kotlin/blob/d3200b2c65b829b85244c4ec4cb19f6e479b06ba/core/util.runtime/src/org/jetbrains/kotlin/utils/addToStdlib.kt#L111
 */
@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public inline fun <reified T : Any> Any?.cast(): T = this as T
