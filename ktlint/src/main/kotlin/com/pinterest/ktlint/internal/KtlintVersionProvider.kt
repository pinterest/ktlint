package com.pinterest.ktlint.internal

import java.util.jar.Manifest
import picocli.CommandLine

/**
 * Dynamically provides current ktlint version.
 */
internal class KtlintVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        var version: String? = javaClass.`package`.implementationVersion
        if (version == null) {
            // JDK 9 regression workaround (https://bugs.openjdk.java.net/browse/JDK-8190987, fixed in JDK 10)
            // (note that version reported by the fallback might not be null if META-INF/MANIFEST.MF is
            // loaded from another JAR on the classpath (e.g. if META-INF/MANIFEST.MF wasn't created as part of the build))
            version = javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")?.run {
                Manifest(this).mainAttributes.getValue("Implementation-Version")
            }
        }

        return version?.let { arrayOf(version) } ?: error("Failed to determine ktlint version")
    }
}
