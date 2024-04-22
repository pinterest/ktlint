package com.pinterest.ktlint.ruleset.standard.rules.importordering

import com.pinterest.ktlint.ruleset.standard.rules.ImportOrderingRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.importordering.PatternEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImportOrderingRuleTest {
    @Test
    fun `Given a list of pattern entries then write the patterns as comma separated string`() {
        val actual =
            ImportOrderingRule.IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY.propertyWriter(
                listOf(
                    PatternEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PatternEntry(packageName = "java", withSubpackages = true, hasAlias = false),
                    PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY,
                ),
            )
        assertThat(actual).isEqualTo("*,java.**,^")
    }
}
