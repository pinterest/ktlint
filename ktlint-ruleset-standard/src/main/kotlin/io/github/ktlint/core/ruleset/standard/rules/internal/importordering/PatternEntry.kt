package io.github.ktlint.core.ruleset.standard.rules.internal.importordering

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

    private fun matchesPackageName(otherPackageName: String): Boolean =
        when {
            this == ALL_OTHER_IMPORTS_ENTRY || this == ALL_OTHER_ALIAS_IMPORTS_ENTRY -> true
            this == BLANK_LINE_ENTRY -> false
            otherPackageName.startsWith(packageName) -> otherPackageName.length == packageName.length || isSubPackageOf(otherPackageName)
            else -> false
        }

    private fun isSubPackageOf(otherPackageName: String): Boolean = withSubpackages && otherPackageName[packageName.length] == '.'

    internal fun matches(import: ImportPath): Boolean = matchesPackageName(import.pathStr.removeSuffix(".*"))

    internal fun isBetterMatchForPackageThan(
        entry: PatternEntry?,
        import: ImportPath,
    ): Boolean =
        when {
            hasAlias != import.hasAlias() || !matchesPackageName(import.pathStr) -> false
            entry == null -> true
            entry.hasAlias != hasAlias -> false
            this == ALL_OTHER_IMPORTS_ENTRY -> false
            entry == ALL_OTHER_IMPORTS_ENTRY -> true
            entry.withSubpackages != withSubpackages -> !withSubpackages
            else -> entry.packageName.count { it == '.' } < packageName.count { it == '.' }
        }

    override fun toString(): String =
        when (this) {
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
        return packageName == other.packageName
    }

    override fun hashCode(): Int {
        var result = withSubpackages.hashCode()
        result = 31 * result + hasAlias.hashCode()
        result = 31 * result + packageName.hashCode()
        return result
    }

    internal companion object {
        val BLANK_LINE_ENTRY: PatternEntry =
            PatternEntry(
                BLANK_LINE_CHAR,
                withSubpackages = true,
                hasAlias = false,
            )

        val ALL_OTHER_IMPORTS_ENTRY: PatternEntry =
            PatternEntry(
                WILDCARD_CHAR,
                withSubpackages = true,
                hasAlias = false,
            )

        val ALL_OTHER_ALIAS_IMPORTS_ENTRY: PatternEntry =
            PatternEntry(
                ALIAS_CHAR,
                withSubpackages = true,
                hasAlias = true,
            )
    }
}
