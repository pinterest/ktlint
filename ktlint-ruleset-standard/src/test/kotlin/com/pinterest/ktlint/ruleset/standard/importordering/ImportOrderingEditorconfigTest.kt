package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule.Companion.IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImportOrderingEditorconfigTest {
    @Test
    fun `import ordering gets written correctly to editorconfig`() {
        val properties: EditorConfigProperties = emptyMap()
        val rule = ImportOrderingRule()
        with(rule) {
            val actual = properties.writeEditorConfigProperty(IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY, CodeStyleValue.intellij_idea)

            assertThat(actual).isEqualTo("*,java.**,javax.**,kotlin.**,^")
        }
    }
}
