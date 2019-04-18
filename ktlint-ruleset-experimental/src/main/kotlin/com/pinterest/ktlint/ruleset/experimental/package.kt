package com.pinterest.ktlint.ruleset.experimental

internal fun <T> List<T>.head() = this.subList(0, this.size - 1)
internal fun <T> List<T>.tail() = this.subList(1, this.size)
