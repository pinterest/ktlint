package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.getKtlintVersion
import picocli.CommandLine

/**
 * Dynamically provides current ktlint version as [CommandLine.IVersionProvider]
 */
internal class KtlintVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val version = getKtlintVersion(KtlintVersionProvider::class.java)
        return version?.let { arrayOf(version) } ?: error("Failed to determine ktlint version")
    }
}
