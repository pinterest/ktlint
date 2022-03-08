package com.pinterest.ktlint.core

import java.io.PrintStream
import java.io.Serializable

/**
 * `ktlint` uses [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to
 * discover all available [ReporterProvider]s on the classpath and so each [ReporterProvider] must be registered using
 * `META-INF/services/com.pinterest.ktlint.core.ReporterProvider`
 * (see `ktlint-reporter-plain/src/main/resources` for an example).
 */
public interface ReporterProvider<T : Reporter> : Serializable {
    public val id: String
    public fun get(out: PrintStream, opt: Map<String, String>): T
}
