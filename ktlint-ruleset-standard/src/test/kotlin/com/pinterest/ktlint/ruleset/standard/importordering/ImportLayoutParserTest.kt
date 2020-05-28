package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import com.pinterest.ktlint.ruleset.standard.internal.importordering.parseImportsLayout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImportLayoutParserTest {

    @Test(expected = IllegalArgumentException::class)
    fun `blank lines in the beginning and end of import list are not allowed`() {
        parseImportsLayout("|,*,|")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `pattern without single wildcard is not allowed`() {
        parseImportsLayout("java.util.List")
    }

    @Test
    fun `parses correctly`() {
        val expected = listOf(
            PatternEntry("android", withSubpackages = true, hasAlias = false),
            PatternEntry.BLANK_LINE_ENTRY,
            PatternEntry("org.junit", withSubpackages = true, hasAlias = false),
            PatternEntry.BLANK_LINE_ENTRY,
            PatternEntry("android", withSubpackages = true, hasAlias = true),
            PatternEntry.ALL_OTHER_IMPORTS_ENTRY,
            PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
        )
        val actual = parseImportsLayout("android.*,|,org.junit.*,|,^android.*,*,^*")

        assertThat(actual).isEqualTo(expected)
    }
}
