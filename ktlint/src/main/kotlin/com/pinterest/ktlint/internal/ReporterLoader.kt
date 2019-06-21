package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.net.URLDecoder
import java.util.LinkedHashMap
import java.util.ServiceLoader
import kotlin.system.exitProcess

fun loadReporter(
    dependencyResolver: Lazy<MavenDependencyResolver>,
    reporters: List<String>,
    debug: Boolean,
    verbose: Boolean,
    color: Boolean,
    stdin: Boolean,
    skipClasspathCheck: Boolean,
    tripped: () -> Boolean
): Reporter {
    val templates = (if (reporters.isEmpty()) listOf("plain") else reporters)
        .map { reporter ->
            val split = reporter.split(",")
            val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
            ReporterTemplate(
                reporterId,
                split.lastOrNull { it.startsWith("artifact=") }?.let { it.split("=")[1] },
                mapOf("verbose" to verbose.toString(), "color" to color.toString()) + parseQuery(
                    rawReporterConfig
                ),
                split.lastOrNull { it.startsWith("output=") }?.let { it.split("=")[1] }
            )
        }
        .distinct()

    val reporterLoader = ServiceLoader.load(ReporterProvider::class.java)
    val reporterProviderById = reporterLoader.associateBy(ReporterProvider::id).let { map ->
        val missingReporters =
            templates.filter { !map.containsKey(it.id) }.mapNotNull { it.artifact }.distinct()
        if (!missingReporters.isEmpty()) {
            val artifactUrls = dependencyResolver.value.resolveArtifacts(missingReporters, debug, skipClasspathCheck)
            val extendedClassLoader = URLClassLoader(artifactUrls.toTypedArray(), Reporter::class.java.classLoader)
            ServiceLoader.load(ReporterProvider::class.java, extendedClassLoader)
                .associateBy(ReporterProvider::id)
        } else map
    }
    if (debug) {
        reporterProviderById.forEach { (id) -> System.err.println("[DEBUG] Discovered reporter \"$id\"") }
    }
    fun ReporterTemplate.toReporter(): Reporter {
        val reporterProvider = reporterProviderById[id]
        if (reporterProvider == null) {
            System.err.println(
                "Error: reporter \"$id\" wasn't found (available: ${
                reporterProviderById.keys.sorted().joinToString(",")
                })"
            )
            exitProcess(1)
        }
        if (debug) {
            System.err.println(
                "[DEBUG] Initializing \"$id\" reporter with $config" + (
                    output?.let { ", output=$it" } ?: ""
                    )
            )
        }
        val stream = if (output != null) {
            File(output).parentFile?.mkdirsOrFail(); PrintStream(output, "UTF-8")
        } else if (stdin) System.err else System.out
        return reporterProvider.get(stream, config)
            .let { reporter ->
                if (output != null) {
                    object : Reporter by reporter {
                        override fun afterAll() {
                            reporter.afterAll()
                            stream.close()
                            if (tripped()) {
                                System.err.println("\"$id\" report written to ${File(output)}")
                            }
                        }
                    }
                } else {
                    reporter
                }
            }
    }
    return Reporter.from(*templates.map { it.toReporter() }.toTypedArray())
}

private fun parseQuery(query: String) = query.split("&")
    .fold(LinkedHashMap<String, String>()) { map, s ->
        if (!s.isEmpty()) {
            s.split("=", limit = 2).let { e ->
                map.put(
                    e[0],
                    URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8")
                )
            }
        }
        map
    }

private data class ReporterTemplate(
    val id: String,
    val artifact: String?,
    val config: Map<String, String>,
    var output: String?
)
