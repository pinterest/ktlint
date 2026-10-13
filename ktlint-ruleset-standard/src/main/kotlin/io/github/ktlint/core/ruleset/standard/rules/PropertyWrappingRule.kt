package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.logger.api.initKtLintKLogger
import io.github.ktlint.core.rule.engine.core.api.ElementType.CALL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.EQ
import io.github.ktlint.core.rule.engine.core.api.ElementType.IDENTIFIER
import io.github.ktlint.core.rule.engine.core.api.ElementType.PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_REFERENCE
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.hasNoMaxLineLengthSuppression
import io.github.ktlint.core.rule.engine.core.api.indentWithoutNewlinePrefix
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.leavesBackwardsIncludingSelf
import io.github.ktlint.core.rule.engine.core.api.leavesForwardsIncludingSelf
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * This rule inserts missing newlines inside a property.
 *
 * Whenever a linebreak is inserted, this rules assumes that the parent node it indented correctly. So the indentation
 * is fixed with respect to indentation of the parent. This is just a simple best effort for the case that the
 * indentation rule is not run.
 *
 * This rule has many similarities with the [ParameterWrappingRule] but some subtle differences.
 */
@SinceKtlint("0.49", SinceKtlint.Status.STABLE)
public class PropertyWrappingRule :
    StandardRule(
        id = "property-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var line = 1
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        line = 1
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig.maxLineLength()
    }

    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType == PROPERTY) {
            rearrangeProperty(node)
        }
    }

    private fun rearrangeProperty(node: ASTNode) {
        require(node.elementType == PROPERTY)

        val fromNode = findFirstNodeAfterIndentOnSameLineAsTheIdentifier(node)

        // Only fix the first child that does not fit
        node.fixChildIfDoesNotFit(COLON, fromNode) { requireNewlineAfterLeaf(it) } ||
            node.fixChildIfDoesNotFit(TYPE_REFERENCE, fromNode) { requireNewlineBeforeLeaf(it) } ||
            node.fixChildIfDoesNotFit(EQ, fromNode) { requireNewlineAfterLeaf(it) } ||
            node.fixChildIfDoesNotFit(CALL_EXPRESSION, fromNode) { requireNewlineBeforeLeaf(it) }
    }

    private fun findFirstNodeAfterIndentOnSameLineAsTheIdentifier(node: ASTNode): ASTNode {
        val nodeFirstChildLeafOrSelf = node.firstChildLeafOrSelf
        val fromNode =
            node
                .findChildByType(IDENTIFIER)
                ?.leavesBackwardsIncludingSelf
                ?.firstOrNull { it.prevLeaf.isWhiteSpaceWithNewline || it == nodeFirstChildLeafOrSelf }
                ?: node
        return fromNode
    }

    private fun ASTNode.fixChildIfDoesNotFit(
        elementType: IElementType,
        fromNode: ASTNode,
        fix: (ASTNode) -> Unit,
    ): Boolean =
        findChildByType(elementType)
            ?.takeIf { it.doesNotFitOnSameLineAs(fromNode) }
            ?.let {
                fix(it)
                true
            }
            ?: false

    private fun ASTNode.doesNotFitOnSameLineAs(fromNode: ASTNode): Boolean =
        hasNoMaxLineLengthSuppression() &&
            parent!!.indentWithoutNewlinePrefix.length + fromNode.sumOfTextLengthUntil(this) > maxLineLength

    private fun ASTNode.sumOfTextLengthUntil(astNode: ASTNode): Int {
        val stopAtLeaf = astNode.lastChildLeafOrSelf
        return leavesForwardsIncludingSelf
            .takeWhile { !it.isWhiteSpaceWithNewline && it.prevLeaf != stopAtLeaf }
            .sumOf { it.textLength }
    }

    private fun requireNewlineBeforeLeaf(node: ASTNode) {
        emit(node.startOffset - 1, """Missing newline before "${node.text}"""") {
            LOGGER.trace { "$line: inserted newline before ${node.text}" }
            node.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
        }
    }

    private fun requireNewlineAfterLeaf(
        nodeAfterWhichNewlineIsRequired: ASTNode,
        nodeToFix: ASTNode = nodeAfterWhichNewlineIsRequired,
    ) {
        emit(nodeAfterWhichNewlineIsRequired.startOffset + 1, """Missing newline after "${nodeAfterWhichNewlineIsRequired.text}"""") {
            LOGGER.trace { "$line: inserted newline after ${nodeAfterWhichNewlineIsRequired.text}" }
            nodeToFix.upsertWhitespaceAfterMe(indentConfig.childIndentOf(nodeToFix))
        }
    }
}

public val PROPERTY_WRAPPING_RULE_ID: RuleId = PropertyWrappingRule().ruleId
