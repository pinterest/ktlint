package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import com.pinterest.ktlint.ruleset.standard.internal.importordering.parseImportsLayout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ImportLayoutParserTest {
    @Test
    fun `blank lines in the beginning and end of import list are not allowed`() {
        assertThatThrownBy {
            parseImportsLayout("|,*,|")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `pattern without single wildcard is not allowed`() {
        assertThatThrownBy {
            parseImportsLayout("java.util.List.*")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parses correctly`() {
        val expected = listOf(
            PatternEntry("android.*", withSubpackages = true, hasAlias = false),
            PatternEntry.BLANK_LINE_ENTRY,
            PatternEntry("org.junit.*", withSubpackages = true, hasAlias = false),
            PatternEntry.BLANK_LINE_ENTRY,
            PatternEntry("android.*", withSubpackages = true, hasAlias = true),
            PatternEntry.ALL_OTHER_IMPORTS_ENTRY,
            PatternEntry("kotlin.io.Closeable.*", withSubpackages = false, hasAlias = false),
            PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY,
        )
        val actual = parseImportsLayout("android.**,|,org.junit.**,|,^android.**,*,kotlin.io.Closeable.*,^")

        assertThat(actual).isEqualTo(expected)
    }
}
