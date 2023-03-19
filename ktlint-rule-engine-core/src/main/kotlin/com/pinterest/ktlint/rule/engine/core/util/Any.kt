package com.pinterest.ktlint.rule.engine.core.util

public inline fun <reified T : Any> Any?.safeAs(): T? = this as? T

public inline fun <reified T : Any> Any?.cast(): T = this as T
