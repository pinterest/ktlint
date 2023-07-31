package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND
import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

private const val KTLINT_JAR = "ktlint"

internal fun <T> Class<T>.loadFromJarFiles(
    urls: List<URL>,
    providerId: (T) -> String,
    customJarProviderCheck: CustomJarProviderCheck,
): Set<T> {
    val providersFromKtlintJars =
        this
            .loadProvidersFromJars(null)
            .also { providers ->
                providers
                    .mapNotNull { it }
                    .forEach {
                        LOGGER.debug { "Discovered $simpleName with id '${providerId(it)}' in ktlint JAR" }
                    }
            }
    val providerIdsFromKtlintJars =
        providersFromKtlintJars
            .map { providerId(it) }
    val providersFromCustomJars =
        urls
            // Remove JAR files which were provided multiple times
            .distinct()
            .map { url ->
                val providers =
                    this
                        .loadProvidersFromJars(url)
                        .filterNot { providerIdsFromKtlintJars.contains(providerId(it)) }
                if (providers.isEmpty()) {
                    if (customJarProviderCheck == ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.error {
                                """
                                JAR file '${url.path}' is missing a class implementing interface '$canonicalName'
                                    KtLint uses a ServiceLoader to dynamically load classes from JAR files specified at the command line of KtLint.
                                    The JAR file below does not contain an implementation of the interface.
                                        Interface: $canonicalName
                                        JAR file : ${url.path}
                                    Check following:
                                      - Does the jar contain an implementation of the interface above?
                                      - Does the jar contain a resource file with name '$canonicalName'?
                                      - Is the resource file located in directory "src/main/resources/META-INF/services"?
                                      - Does the resource file contain the fully qualified class name of the class implementing the interface above?
                                """.trimIndent()
                            }
                        } else {
                            LOGGER.error {
                                "JAR file '${url.path}' is missing a class implementing interface '$canonicalName' (run in debug mode " +
                                    "for more information)"
                            }
                        }
                        exitKtLintProcess(1)
                    }
                    providers
                } else {
                    providers
                        .mapNotNull {
                            if (customJarProviderCheck == ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.error {
                                        """
                                        JAR file '${url.path}' contains a class implementing an unsupported interface '$canonicalName'
                                            KtLint uses a ServiceLoader to dynamically load classes from JAR files specified at the command line of KtLint.
                                            The JAR file below contains an implementation of an interface which is no longer supported by this version of
                                            KtLint. Please contact the maintainer of this JAR file (not maintained by KtLint) to upgrade the JAR file so
                                            that you can use it again.
                                                Interface: $canonicalName
                                                JAR file : ${url.path}
                                        """.trimIndent()
                                    }
                                } else {
                                    LOGGER.error {
                                        "JAR file '${url.path}' contains a class implementing an unsupported interface '$canonicalName' " +
                                            "(run in debug mode for more information)"
                                    }
                                }
                                exitKtLintProcess(1)
                            } else {
                                LOGGER.debug {
                                    """
                                    Discovered $simpleName with id '${providerId(it)}' in JAR file '${url.path}'
                                        KtLint uses a ServiceLoader to dynamically load classes from JAR files specified at the command line of KtLint.
                                        The JAR file below contains an implementation of an interface which is supported by this version of ktlint:
                                            Interface: $canonicalName
                                            Id       : ${providerId(it)}
                                            JAR file : ${url.path}
                                    """.trimIndent()
                                }
                                it
                            }
                        }
                }
            }.flatten()
            .toSet()
    return providersFromKtlintJars
        .plus(providersFromCustomJars)
        .mapNotNull { it }
        .toSet()
}

private fun <T> Class<T>.loadProvidersFromJars(url: URL?): Set<T> =
    try {
        ServiceLoader
            .load(
                this,
                URLClassLoader(url.toArray()),
            ).toSet()
    } catch (e: ServiceConfigurationError) {
        LOGGER.warn { "Error while loading JAR file '${url.jarFilePath()}':\n${e.printStackTrace()}" }
        emptySet()
    }

private fun URL?.toArray() =
    this
        ?.let { arrayOf(this) }
        .orEmpty()

private fun URL?.jarFilePath() =
    this
        ?.path
        ?: KTLINT_JAR

internal enum class CustomJarProviderCheck {
    /**
     * Log an error and exit ktlint when the JAR file does not contain a required provider class
     */
    ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING,

    /**
     * Log an error and exit when the JAR file contains a deprecated provider class
     */
    ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND,
}
