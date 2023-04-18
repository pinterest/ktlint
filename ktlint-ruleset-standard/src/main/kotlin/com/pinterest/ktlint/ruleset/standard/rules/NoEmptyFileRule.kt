package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoEmptyFileRule :
    StandardRule(id = "no-empty-file", usesEditorConfigProperties = setOf(NO_EMPTY_FILE_PROPERTY)) {
    private var noEmptyFile = NO_EMPTY_FILE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        noEmptyFile = editorConfig[NO_EMPTY_FILE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (noEmptyFile) {
            node
                .takeIf { it.elementType == ElementType.FILE }
                ?.takeIf { it.textLength == TEXT_LENGTH_EMPTY_FILE_CONTAINS }
                ?.let {
                    val filePath = it.psi.containingFile.virtualFile.name
                    val fileName =
                        filePath
                            .replace("\\", "/") // Ensure compatibility with Windows OS
                            .substringAfterLast("/")
                    emit(
                        0,
                        "File `$fileName` should not be empty",
                        false,
                    )
                }
        }
    }

    public companion object {
        private const val TEXT_LENGTH_EMPTY_FILE_CONTAINS: Int = 0

        public val NO_EMPTY_FILE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "no_empty_file",
                        "Define whether empty files are allowed",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        setOf(true.toString(), false.toString()),
                    ),
                defaultValue = false,
            )
    }
}
