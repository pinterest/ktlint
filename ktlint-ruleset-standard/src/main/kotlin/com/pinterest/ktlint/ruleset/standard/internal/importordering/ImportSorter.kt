package com.pinterest.ktlint.ruleset.standard.internal.importordering

import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Sorts the imports according to the order specified in [patterns] + alphabetically.
 *
 * Adopted from https://github.com/JetBrains/kotlin/blob/a270ee094c4d7b9520e0898a242bb6ce4dfcad7b/idea/src/org/jetbrains/kotlin/idea/util/ImportPathComparator.kt#L15
 */
internal class ImportSorter(
    val patterns: List<PatternEntry>,
) : Comparator<KtImportDirective> {

    override fun compare(import1: KtImportDirective, import2: KtImportDirective): Int {
        val importPath1 = import1.importPath!!
        val importPath2 = import2.importPath!!

        return compareValuesBy(
            importPath1,
            importPath2,
            { import -> findImportIndex(import) },
            { import -> import.toString().replace("`", "") },
        )
    }

    fun findImportIndex(path: ImportPath): Int {
        var bestIndex: Int = -1
        var bestEntryMatch: PatternEntry? = null
        var allOtherAliasIndex = -1
        var allOtherIndex = -1

        for ((index, entry) in patterns.withIndex()) {
            if (entry == PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY) {
                allOtherAliasIndex = index
            }
            if (entry == PatternEntry.ALL_OTHER_IMPORTS_ENTRY) {
                allOtherIndex = index
            }
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        if (bestIndex == -1 && path.hasAlias() && allOtherAliasIndex == -1 && allOtherIndex != -1) {
            // if no layout for alias imports specified, put them among all others
            bestIndex = allOtherIndex
        }
        return bestIndex
    }
}
