package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.ktlintVersion
import picocli.CommandLine

/**
 * Dynamically provides current ktlint version as [CommandLine.IVersionProvider]
 */
internal class KtlintVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> =
        ktlintVersion(KtlintVersionProvider::class.java)
            ?.let { version -> arrayOf(version) }
            ?: error("Failed to determine ktlint version")
}
