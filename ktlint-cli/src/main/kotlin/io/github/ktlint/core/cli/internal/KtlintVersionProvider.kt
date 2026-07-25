package io.github.ktlint.core.cli.internal

import io.github.ktlint.core.cli.reporter.core.api.ktlintVersion

/**
 * Dynamically provides current ktlint version
 */
internal class KtlintVersionProvider {
    val version: String =
        ktlintVersion(KtlintVersionProvider::class.java)
            ?: error("Failed to determine ktlint version")
}
