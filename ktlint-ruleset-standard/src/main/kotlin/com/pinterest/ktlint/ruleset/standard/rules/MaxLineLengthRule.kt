package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY_OFF
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

@SinceKtlint("0.9", STABLE)
public class MaxLineLengthRule :
    StandardRule(
        id = "max-line-length",
        visitorModifiers =
            setOf(
                VisitorModifier.RunAfterRule(
                    // This rule should run after all other rules. Each time a rule visitor is modified with
                    // RunAsLateAsPossible, it needs to be checked that this rule still runs after that new rule or that it
                    // won't be affected by that rule.
                    ruleId = TRAILING_COMMA_ON_CALL_SITE_RULE_ID,
                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                ),
                VisitorModifier.RunAfterRule(
                    // This rule should run after all other rules. Each time a rule visitor is modified with
                    // RunAsLateAsPossible, it needs to be checked that this rule still runs after that new rule or that it
                    // won't be affected by that rule.
                    ruleId = TRAILING_COMMA_ON_DECLARATION_SITE_RULE_ID,
                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                ),
                VisitorModifier.RunAsLateAsPossible,
            ),
        usesEditorConfigProperties =
            setOf(
                MAX_LINE_LENGTH_PROPERTY,
                IGNORE_BACKTICKED_IDENTIFIER_PROPERTY,
            ),
    ) {
    private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue
    private var ignoreBackTickedIdentifier = IGNORE_BACKTICKED_IDENTIFIER_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        ignoreBackTickedIdentifier = editorConfig[IGNORE_BACKTICKED_IDENTIFIER_PROPERTY]
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        if (maxLineLength == MAX_LINE_LENGTH_PROPERTY_OFF) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isWhiteSpace()) {
            return
        }
        node
            .takeIf { it is LeafPsiElement }
            ?.takeIf { it.nextLeaf() == null || it.nextLeaf().isWhiteSpaceWithNewline() }
            ?.takeIf { it.lineLength() > maxLineLength }
            ?.takeUnless { it.isPartOf(KtPackageDirective::class) }
            ?.takeUnless { it.isPartOf(KtImportDirective::class) }
            ?.takeUnless { it.isPartOf(KDoc::class) }
            ?.takeUnless { it.isPartOfRawMultiLineString() }
            ?.takeUnless { it.isLineOnlyContainingSingleTemplateString() }
            ?.takeUnless { it.isLineOnlyContainingComment() }
            ?.let { lastNodeOnLine ->
                // Calculate the offset at the last possible position at which the newline should be inserted on the line
                val offset = node.leavesOnLine().first().startOffset + maxLineLength + 1
                emit(
                    offset,
                    "Exceeded max line length ($maxLineLength)",
                    false,
                )
            }
    }

    private fun ASTNode.lineLength() =
        leavesOnLine()
            .sumOf {
                when {
                    it.isWhiteSpaceWithNewline() -> {
                        it.text.substringAfterLast('\n').length
                    }

                    it.elementType == IDENTIFIER &&
                        it.text.matches(BACKTICKED_IDENTIFIER_REGEX) &&
                        ignoreBackTickedIdentifier
                    -> {
                        0
                    }

                    else -> {
                        it.textLength
                    }
                }
            }

    private fun ASTNode.isPartOfRawMultiLineString() =
        parent(STRING_TEMPLATE, strict = false)
            ?.let { it.firstChildNode.text == "\"\"\"" && it.textContains('\n') } == true

    private fun ASTNode.isLineOnlyContainingSingleTemplateString() =
        treeParent
            ?.takeIf { it.elementType == STRING_TEMPLATE }
            ?.let { stringTemplate ->
                stringTemplate
                    .prevLeaf()
                    .let { leafBeforeStringTemplate ->
                        leafBeforeStringTemplate == null || leafBeforeStringTemplate.isWhiteSpaceWithNewline()
                    }
            }
            ?: false

    private fun ASTNode.isLineOnlyContainingComment() =
        isPartOf(PsiComment::class) &&
            (prevLeaf() == null || prevLeaf().isWhiteSpaceWithNewline())

    public companion object {
        public val IGNORE_BACKTICKED_IDENTIFIER_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_ignore_back_ticked_identifier",
                        "Defines whether the backticked identifier (``) should be ignored",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        setOf(true.toString(), false.toString()),
                    ),
                defaultValue = false,
            )
        private val BACKTICKED_IDENTIFIER_REGEX = Regex("`.*`")
    }
}

public val MAX_LINE_LENGTH_RULE_ID: RuleId = MaxLineLengthRule().ruleId
