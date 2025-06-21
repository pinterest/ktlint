package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 *
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class EnumWrappingRule :
    StandardRule(
        id = "enum-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { node.elementType == CLASS }
            ?.takeIf { node.hasModifier(ElementType.ENUM_KEYWORD) }
            ?.findChildByType(CLASS_BODY)
            ?.let { classBody ->
                visitEnumClass(classBody, emit)
            }
    }

    private fun visitEnumClass(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == CLASS_BODY)

        val commentBeforeFirstEnumEntry = wrapCommentBeforeFirstEnumEntry(node, emit)
        if (commentBeforeFirstEnumEntry || node.isMultiline() || node.hasAnnotatedEnumEntry() || node.hasCommentedEnumEntry()) {
            wrapEnumEntries(node, emit)
            wrapClosingBrace(node, emit)
        }
        addBlankLineBetweenEnumEntriesAndOtherDeclarations(node, emit)
    }

    private fun wrapCommentBeforeFirstEnumEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ): Boolean {
        val firstEnumEntry = node.findChildByType(ENUM_ENTRY)?.firstChildLeafOrSelf()
        if (firstEnumEntry != null) {
            node
                .firstChildLeafOrSelf()
                .leavesIncludingSelf()
                .takeWhile { it != firstEnumEntry }
                .firstOrNull { it.isPartOfComment() }
                ?.let { commentBeforeFirstEnumEntry ->
                    val expectedIndent = indentConfig.childIndentOf(node)
                    if (commentBeforeFirstEnumEntry.prevLeaf()?.text != expectedIndent) {
                        emit(node.startOffset, "Expected a (single) newline before comment", true)
                            .ifAutocorrectAllowed {
                                commentBeforeFirstEnumEntry.upsertWhitespaceBeforeMe(indentConfig.siblingIndentOf(node))
                            }
                        return true
                    }
                }
        }
        return false
    }

    private fun ASTNode.isMultiline() = text.contains('\n')

    private fun ASTNode.hasAnnotatedEnumEntry() =
        children()
            .filter { it.elementType == ENUM_ENTRY }
            .any { it.isAnnotated() }

    private fun ASTNode.isAnnotated(): Boolean =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun ASTNode.hasCommentedEnumEntry() = children().any { it.containsCommentInEnumEntry() }

    private fun ASTNode.containsCommentInEnumEntry() = children().any { it.isPartOfComment() }

    private fun wrapEnumEntries(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .children()
            .filter { it.elementType == ENUM_ENTRY }
            .forEach { enumEntry ->
                wrapEnumEntry(enumEntry, emit)
            }
    }

    private fun wrapEnumEntry(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .prevLeaf { !it.isPartOfComment() && !it.isWhiteSpaceWithoutNewline20 }
            ?.takeUnless { it.isWhiteSpaceWithNewline20 }
            ?.let { prevLeaf ->
                emit(node.startOffset, "Enum entry should start on a separate line", true)
                    .ifAutocorrectAllowed {
                        prevLeaf.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(node))
                    }
            }
    }

    private fun wrapClosingBrace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(RBRACE)
            ?.let { rbrace ->
                val prevLeaf = rbrace.prevLeaf()
                val expectedIndent = indentConfig.parentIndentOf(node)
                if (prevLeaf?.text != expectedIndent) {
                    emit(rbrace.startOffset, "Expected newline before '}'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(expectedIndent)
                        }
                }
            }
    }

    private fun addBlankLineBetweenEnumEntriesAndOtherDeclarations(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .children()
            .lastOrNull { it.elementType == ENUM_ENTRY }
            ?.nextSibling { !it.isPartOfComment() }
            ?.takeUnless { it.nextCodeSibling()?.elementType == RBRACE }
            ?.let { nextSibling ->
                val expectedIndent = "\n".plus(indentConfig.siblingIndentOf(node))
                if (nextSibling.text != expectedIndent) {
                    emit(nextSibling.startOffset + 1, "Expected blank line between enum entries and other declaration(s)", true)
                        .ifAutocorrectAllowed {
                            nextSibling.upsertWhitespaceBeforeMe(expectedIndent)
                        }
                }
            }
    }
}

public val ENUM_WRAPPING_RULE_ID: RuleId = EnumWrappingRule().ruleId
