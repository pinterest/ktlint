package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(FeatureInAlphaState::class)
class ImportOrderingEditorconfigTest {

    @Test
    fun `import ordering gets written correctly to editorconfig`() {
        val properties: EditorConfigProperties = emptyMap()
        val rule = ImportOrderingRule()
        with(rule) {
            val raw = properties.writeEditorConfigProperty(ImportOrderingRule.ideaImportsLayoutProperty, false)
            assertThat(raw).isEqualTo("*,java.**,javax.**,kotlin.**,^")
        }
    }
}
