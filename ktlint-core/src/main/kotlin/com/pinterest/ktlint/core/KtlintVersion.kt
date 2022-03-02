package com.pinterest.ktlint.core

import java.util.jar.Manifest

/**
 * Dynamically provides current ktlint version
 *
 * Note: The fallback reading from `/META-INF/MANIFEST.MF` is a
 * JDK 9 regression workaround (https://bugs.openjdk.java.net/browse/JDK-8190987, fixed in JDK 10)
 * (note that version reported by the fallback might not be null if META-INF/MANIFEST.MF is
 * loaded from another JAR on the classpath (e.g. if META-INF/MANIFEST.MF wasn't created as part of the build))
 */
public fun <T> ktlintVersion(javaClass: Class<T>): String? =
    javaClass.`package`.implementationVersion ?: getManifestVersion(javaClass)

private fun <T> getManifestVersion(javaClass: Class<T>) =
    javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
        ?.run {
            Manifest(this).mainAttributes.getValue("Implementation-Version")
        }
