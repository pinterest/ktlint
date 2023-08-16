package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoEmptyFileRule :
    StandardRule(id = "no-empty-file"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.isRoot() }
            ?.takeIf { it.isEmptyFile() }
            ?.let { emit(0, "File '${node.getFileName()}' should not be empty", false) }
    }

    private fun ASTNode.getFileName() =
        psi
            .containingFile
            .virtualFile
            .name
            .replace("\\", "/") // Ensure compatibility with Windows OS
            .substringAfterLast("/")

    private fun ASTNode.isEmptyFile(): Boolean =
        null ==
            children()
                .firstOrNull {
                    !it.isWhiteSpace() &&
                        !it.isPartOfComment() &&
                        it.elementType != ElementType.PACKAGE_DIRECTIVE &&
                        it.elementType != ElementType.IMPORT_LIST &&
                        !(it.elementType == ElementType.SCRIPT && it.text.isBlank())
                }
}

public val NO_EMPTY_FILE_RULE_ID: RuleId = NoEmptyFileRule().ruleId
