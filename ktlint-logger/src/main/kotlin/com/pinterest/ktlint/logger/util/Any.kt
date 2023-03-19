package com.pinterest.ktlint.logger.util

public inline fun <reified T : Any> Any?.safeAs(): T? = this as? T

public inline fun <reified T : Any> Any?.cast(): T = this as T
