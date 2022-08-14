package com.pinterest.ktlint.ruleset.standard.internal.importordering

import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Represents an entry in the imports layout pattern. Contains matching logic for imports.
 *
 * Adopted from https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinPackageEntry.kt#L10
 */
public class PatternEntry(
    packageName: String,
    public val withSubpackages: Boolean,
    public val hasAlias: Boolean,
) {

    private val packageName = packageName.removeSuffix(".*")

    private fun matchesPackageName(otherPackageName: String): Boolean {
        if (this == ALL_OTHER_IMPORTS_ENTRY || this == ALL_OTHER_ALIAS_IMPORTS_ENTRY) return true
        if (this == BLANK_LINE_ENTRY) return false

        if (otherPackageName.startsWith(packageName)) {
            if (otherPackageName.length == packageName.length) {
                return true
            }
            if (withSubpackages && otherPackageName[packageName.length] == '.') {
                return true
            }
        }
        return false
    }

    internal fun matches(import: ImportPath): Boolean {
        return matchesPackageName(import.pathStr.removeSuffix(".*"))
    }

    internal fun isBetterMatchForPackageThan(entry: PatternEntry?, import: ImportPath): Boolean {
        if (hasAlias != import.hasAlias() || !matchesPackageName(import.pathStr)) return false
        if (entry == null) return true

        if (entry.hasAlias != hasAlias) return false
        // Any matched package is better than ALL_OTHER_IMPORTS_ENTRY
        if (this == ALL_OTHER_IMPORTS_ENTRY) return false
        if (entry == ALL_OTHER_IMPORTS_ENTRY) return true

        if (entry.withSubpackages != withSubpackages) return !withSubpackages

        return entry.packageName.count { it == '.' } < packageName.count { it == '.' }
    }

    override fun toString(): String = when (this) {
        ALL_OTHER_IMPORTS_ENTRY -> WILDCARD_CHAR
        ALL_OTHER_ALIAS_IMPORTS_ENTRY -> ALIAS_CHAR
        else -> "$packageName.$WILDCARD_CHAR" + if (withSubpackages) WILDCARD_CHAR else ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatternEntry

        if (withSubpackages != other.withSubpackages) return false
        if (hasAlias != other.hasAlias) return false
        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = withSubpackages.hashCode()
        result = 31 * result + hasAlias.hashCode()
        result = 31 * result + packageName.hashCode()
        return result
    }

    public companion object {
        public val BLANK_LINE_ENTRY: PatternEntry =
            PatternEntry(
                BLANK_LINE_CHAR,
                withSubpackages = true,
                hasAlias = false,
            )

        public val ALL_OTHER_IMPORTS_ENTRY: PatternEntry =
            PatternEntry(
                WILDCARD_CHAR,
                withSubpackages = true,
                hasAlias = false,
            )

        public val ALL_OTHER_ALIAS_IMPORTS_ENTRY: PatternEntry =
            PatternEntry(
                ALIAS_CHAR,
                withSubpackages = true,
                hasAlias = true,
            )
    }
}
