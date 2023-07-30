package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.ReporterAggregator.ReporterConfigurationElement.ARTIFACT
import com.pinterest.ktlint.cli.internal.ReporterAggregator.ReporterConfigurationElement.OUTPUT
import com.pinterest.ktlint.cli.reporter.baseline.Baseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.net.URLDecoder

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

internal class ReporterAggregator(
    private val baseline: Baseline,
    /**
     * Each reporter configuration consists of following elements:
     *   - id: mandatory id of the reporter. This should be an id of a reporter which is provided by Ktlint, or in case of custom reporter
     *     the id of a reporter defined in the given artifact.
     *   - additional config: optional configuration for the reporter. It must be separated from the id with a '?'.
     *   - artifact: path to a JAR file containing the custom reporter. It has to be specified after the id as
     *     ',artifact=<path-to-JAR-file>'
     *   - output: path to file in which to store the output of the reporter
     * Examples:
     *   - "plain?group_by_file"
     *   - "custom?additionalConfig,artifact=/path/to/JAR-file,output=/path/to/output-file"
     */
    private val reporterConfigurations: List<String>,
    private val color: Boolean,
    private val colorName: String,
    private val stdin: Boolean,
    private val format: Boolean,
    private val relative: Boolean,
) {
    fun aggregatedReporter(): ReporterV2 {
        val configurations =
            reporterConfigurations
                .ifEmpty { listOf("plain") }
                .map { parseReporterConfiguration(it) }
                .distinct()
                .toMutableList()
                .apply {
                    if (baseline.status == Baseline.Status.INVALID || baseline.status == Baseline.Status.NOT_FOUND) {
                        this.add(
                            ReporterConfiguration("baseline", null, emptyMap(), baseline.path),
                        )
                    }
                }

        val availableReporterProviders =
            configurations
                .mapNotNull { it.artifact }
                .toFilesURIList()
                .let { loadReporters(it) }

        return configurations
            .map { reporterConfiguration ->
                availableReporterProviders
                    .getOrFail(reporterConfiguration)
                    .toReporterV2(reporterConfiguration)
            }.let { reporters -> AggregatedReporter(reporters) }
    }

    private fun parseReporterConfiguration(configuration: String) =
        parseReporterConfigurationString(configuration)
            .let { config ->
                config.copy(
                    additionalConfig =
                        config.additionalConfig.plus(
                            mapOf(
                                "color" to color.toString(),
                                "color_name" to colorName,
                                "format" to format.toString(),
                            ),
                        ),
                )
            }

    private fun parseReporterConfigurationString(configuration: String): ReporterConfiguration {
        val configurationElements = configuration.split(",")
        return ReporterConfiguration(
            id = configurationElements[0].substringBefore("?"),
            additionalConfig = parseQuery(configurationElements[0].substringAfter("?")),
            artifact = configurationElements.configurationOption(ARTIFACT),
            output = configurationElements.configurationOption(OUTPUT),
        )
    }

    private enum class ReporterConfigurationElement {
        ARTIFACT,
        OUTPUT,
    }

    private fun List<String>.configurationOption(reporterConfigurationElement: ReporterConfigurationElement) =
        lastOrNull { it.startsWith("${reporterConfigurationElement.name.lowercase()}=") }
            ?.substringAfter("=")

    private fun parseQuery(query: String?) =
        query
            ?.split("&")
            ?.fold(LinkedHashMap<String, String>()) { map, s ->
                if (s.isNotEmpty()) {
                    s.split("=", limit = 2).let { e ->
                        map.put(
                            e[0],
                            URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8"),
                        )
                    }
                }
                map
            }.orEmpty()

    private fun Set<ReporterProviderV2<*>>.getOrFail(reporterConfiguration: ReporterConfiguration): ReporterProviderV2<*> {
        val reporterProviderV2 = this.firstOrNull { it.id == reporterConfiguration.id }
        if (reporterProviderV2 == null) {
            LOGGER.error {
                this
                    .map { it.id }
                    .sorted()
                    .joinToString(
                        separator = ", ",
                        prefix = "reporter \"${reporterConfiguration.id}\" wasn't found (available: ",
                        postfix = ")",
                    )
            }
            exitKtLintProcess(1)
        }
        return reporterProviderV2
    }

    private fun ReporterProviderV2<*>.toReporterV2(reporterConfiguration: ReporterConfiguration): ReporterV2 {
        LOGGER.debug {
            "Initializing \"${reporterConfiguration.id}\" reporter with ${reporterConfiguration.additionalConfig}" +
                (reporterConfiguration.output?.let { ", output=$it" } ?: "")
        }
        val stream =
            when {
                reporterConfiguration.output != null -> {
                    File(reporterConfiguration.output).parentFile?.mkdirsOrFail()
                    PrintStream(reporterConfiguration.output, "UTF-8")
                }

                stdin -> {
                    System.err
                }

                else -> {
                    System.out
                }
            }
        return get(stream, reporterConfiguration.additionalConfig)
            .let { reporterV2 ->
                if (reporterConfiguration.output != null) {
                    object : ReporterV2 by reporterV2 {
                        override fun afterAll() {
                            reporterV2.afterAll()
                            stream.close()
                            LOGGER.info {
                                val outputLocation = File(reporterConfiguration.output).absoluteFile.location(relative)
                                "\"${reporterConfiguration.id}\" report written to $outputLocation"
                            }
                        }
                    }
                } else {
                    reporterV2
                }
            }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !isDirectory) {
            throw IOException("Unable to create \"${this}\" directory")
        }
    }

    private data class ReporterConfiguration(
        val id: String,
        val artifact: String?,
        val additionalConfig: Map<String, String>,
        val output: String?,
    )

    private class AggregatedReporter(
        val reporters: List<ReporterV2>,
    ) : ReporterV2 {
        override fun beforeAll() = reporters.forEach(ReporterV2::beforeAll)

        override fun before(file: String) = reporters.forEach { it.before(file) }

        override fun onLintError(
            file: String,
            ktlintCliError: KtlintCliError,
        ) = reporters.forEach { it.onLintError(file, ktlintCliError) }

        override fun after(file: String) = reporters.forEach { it.after(file) }

        override fun afterAll() = reporters.forEach(ReporterV2::afterAll)
    }
}
