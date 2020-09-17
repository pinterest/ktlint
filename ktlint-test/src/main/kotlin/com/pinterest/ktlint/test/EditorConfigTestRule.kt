package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import java.io.File
import org.ec4j.core.model.PropertyType
import org.junit.rules.TemporaryFolder

/**
 * Helper [org.junit.rules.TestRule] allows to write `.editorconfig` file content
 * for test case.
 */
@FeatureInAlphaState
public class EditorConfigTestRule : TemporaryFolder() {

    /**
     * Write new `.editorconfig` file.
     *
     * @param content map of `.editorconfig` [PropertyType] to expected value `String` representation
     * to write under `[*.{kt,kts}] block
     * @param lintedFileExtension linted file extension, default is `.kt`
     * @return pass returned fake lint file path to `KtLint`,
     * so written `.editorconfig` will be picked up for linting
     */
    public fun writeToEditorConfig(
        content: Map<PropertyType<*>, String>,
        lintedFileExtension: String = ".kt"
    ): File {
        val editorConfigFile = File(root, ".editorconfig")
        editorConfigFile.writeText(
            "[*.{kt,kts}]${System.lineSeparator()}"
                .plus(
                    content
                        .map { entry ->
                            "${entry.key.name} = ${entry.value}"
                        }
                        .joinToString(System.lineSeparator())
                )
        )
        return File(editorConfigFile.parent, "test$lintedFileExtension")
    }
}
