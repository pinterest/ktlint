package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.50", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class NoEmptyFileRule : StandardRule(id = "no-empty-file") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
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
