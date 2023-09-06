package com.pinterest.ktlint.cli.environment

import java.util.TreeMap

/**
 * An immutable OS environment, with support for Windows-specific
 * case-insensitive keys (i.e. `OsEnvironment()["PATH"]` and
 * `OsEnvironment()["Path"]` will return the same non-`null` value on Windows).
 */
internal class OsEnvironment private constructor(
    initial: Map<String, String>,
    environment: MutableMap<String, String>,
) : Map<String, String> by environment {
    /**
     * Creates a new OS environment, defaulting to the values returned by
     * [System.getenv].
     *
     * @see System.getenv
     */
    constructor(initial: Map<String, String> = System.getenv()) : this(
        initial,
        environment = emptyEnvironment(),
    )

    init {
        environment.putAll(initial)
    }

    companion object {
        private fun isWindows(): Boolean =
            System
                .getProperty("os.name")
                .startsWith("Windows")

        private fun emptyEnvironment(): MutableMap<String, String> =
            when {
                isWindows() -> CaseInsensitiveMap()
                else -> mutableMapOf()
            }
    }
}

private class CaseInsensitiveMap<V : Any> :
    TreeMap<String, V>(java.lang.String.CASE_INSENSITIVE_ORDER),
    MutableMap<String, V>
