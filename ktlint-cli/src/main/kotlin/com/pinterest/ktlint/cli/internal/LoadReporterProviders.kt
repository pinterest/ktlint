package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.cli.internal.CustomJarProviderCheck.ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING
import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.net.URL

internal fun loadReporters(urls: List<URL>): Set<ReporterProviderV2<*>> {
    // Code below is kept around as reference for future deprecation of current ReporterProvider
    // An error about finding a deprecated ReporterProvider is more important than reporting an error about a missing ReporterProviderV2
    // ReporterProvider::class.java.loadFromJarFiles(urls, providerId = { it.id }, ERROR_WHEN_DEPRECATED_PROVIDER_IS_FOUND)

    return ReporterProviderV2::class.java.loadFromJarFiles(urls, providerId = { it.id }, ERROR_WHEN_REQUIRED_PROVIDER_IS_MISSING)
}
