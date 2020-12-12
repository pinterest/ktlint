package com.pinterest.ktlint.ruleset.standard.internal.importordering

internal const val BLANK_LINE_CHAR = "|"
internal const val WILDCARD_CHAR = "*"
internal const val ALIAS_CHAR = "^"

/**
 * Adapted from https://github.com/JetBrains/intellij-kotlin/blob/73b5a484198f02518c9ece2fb453d27cead680fb/idea/src/org/jetbrains/kotlin/idea/formatter/KotlinPackageEntryTableAccessor.kt#L27-L43
 */
internal fun parseImportsLayout(importsLayout: String): List<PatternEntry> {
    val importsList = importsLayout.split(",").onEach { it.trim() }

    if (importsList.first() == BLANK_LINE_CHAR || importsList.last() == BLANK_LINE_CHAR) {
        throw IllegalArgumentException("Blank lines are not supported in the beginning or end of import list")
    }

    if (WILDCARD_CHAR !in importsList) {
        throw IllegalArgumentException("<all other imports> symbol (\"*\") must be present in the custom imports layout")
    }

    return importsList.map {
        var import = it
        if (import == BLANK_LINE_CHAR) {
            return@map PatternEntry.BLANK_LINE_ENTRY
        } else {
            var hasAlias = false
            var withSubpackages = false
            if (import.startsWith(ALIAS_CHAR)) {
                import = import.substring(1).trim()
                hasAlias = true
            }
            if (import.endsWith(WILDCARD_CHAR + WILDCARD_CHAR)) { // java.**
                import = import.substringBeforeLast(WILDCARD_CHAR)
                withSubpackages = true
            }
            return@map if (import == WILDCARD_CHAR) { // *
                PatternEntry.ALL_OTHER_IMPORTS_ENTRY
            } else if (import.isEmpty() && hasAlias) { // ^
                PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
            } else {
                PatternEntry(import, withSubpackages, hasAlias)
            }
        }
    }
}
