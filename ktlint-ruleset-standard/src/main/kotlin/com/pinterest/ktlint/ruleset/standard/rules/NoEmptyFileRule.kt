package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoEmptyFileRule :
    StandardRule(id = "no-empty-file", usesEditorConfigProperties = setOf(NO_EMPTY_FILE_PROPERTY)),
    Rule.Experimental {
    private var noEmptyFile = NO_EMPTY_FILE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        noEmptyFile = editorConfig[NO_EMPTY_FILE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!noEmptyFile) return

        node
            .takeIf { it.isRoot() }
            ?.takeIf { it.isEmptyFile() }
            ?.let {
                val filePath =
                    node.psi.containingFile.virtualFile.name
                        .replace("\\", "/") // Ensure compatibility with Windows OS
                emit(0, "File `$filePath` should not be empty", false)
            }
    }

    private fun ASTNode.isEmptyFile(): Boolean {
        if (text.isBlank()) return true

        return this.children()
            .toList()
            .filter {
                !it.isWhiteSpace() &&
                    it.elementType != ElementType.PACKAGE_DIRECTIVE &&
                    it.elementType != ElementType.IMPORT_LIST
            }.isEmpty()
    }

    public companion object {
        public val NO_EMPTY_FILE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "no_empty_file",
                        "Define whether empty files are allowed",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        setOf(true.toString(), false.toString()),
                    ),
                defaultValue = true,
            )
    }
}
