package com.pinterest.ktlint.ruleset.standard.internal.importordering

internal const val BLANK_LINE_CHAR = "|"
internal const val WILDCARD_CHAR = "*"
internal const val ALIAS_CHAR = "^" // TODO: replace with a proper char, once implemented on IDEA's side

/**
 * Adopted from https://github.com/JetBrains/intellij-community/blob/70fd799e94246f2c0fe924763ed892765c0dff9a/java/java-impl/src/com/intellij/psi/codeStyle/JavaPackageEntryTableAccessor.java#L25
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
            if (import.endsWith(WILDCARD_CHAR)) {
                withSubpackages = true
            }
            return@map if (import == WILDCARD_CHAR) {
                if (hasAlias) PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY else PatternEntry.ALL_OTHER_IMPORTS_ENTRY
            } else {
                PatternEntry(import, withSubpackages, hasAlias)
            }
        }
    }
}
