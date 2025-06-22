package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
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
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine20
import com.pinterest.ktlint.rule.engine.core.api.lineLengthWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

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
        maxLineLength = editorConfig.maxLineLength()
        if (maxLineLength == MAX_LINE_LENGTH_PROPERTY_OFF) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpace20) {
            return
        }
        node
            .takeIf { it is LeafPsiElement }
            ?.takeIf { it.nextLeaf() == null || it.nextLeaf().isWhiteSpaceWithNewline20 }
            ?.takeIf { it.lineLength() > maxLineLength }
            ?.takeUnless { it.isPartOf(ElementType.PACKAGE_DIRECTIVE) }
            ?.takeUnless { it.isPartOf(ElementType.IMPORT_DIRECTIVE) }
            ?.takeUnless { it.isPartOf(ElementType.KDOC) }
            ?.takeUnless { it.isPartOfRawMultiLineString() }
            ?.takeUnless { it.isLineOnlyContainingSingleTemplateString() }
            ?.takeUnless { it.elementType == COMMA && it.prevLeaf()?.isLineOnlyContainingSingleTemplateString() ?: false }
            ?.takeUnless { it.isLineOnlyContainingComment() }
            ?.let {
                // Calculate the offset at the last possible position at which the newline should be inserted on the line
                val offset =
                    node
                        .leavesOnLine20
                        .first()
                        .startOffset
                        .plus(maxLineLength + 1)
                emit(
                    offset,
                    "Exceeded max line length ($maxLineLength)",
                    false,
                )
            }
    }

    private fun ASTNode.lineLength() =
        leavesOnLine20
            .filterNot {
                ignoreBackTickedIdentifier &&
                    it.elementType == IDENTIFIER &&
                    it.text.matches(BACKTICKED_IDENTIFIER_REGEX)
            }.lineLengthWithoutNewlinePrefix()

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
                        leafBeforeStringTemplate == null || leafBeforeStringTemplate.isWhiteSpaceWithNewline20
                    }
            }
            ?: false

    private fun ASTNode.isLineOnlyContainingComment() =
        isPartOfComment20 &&
            (prevLeaf() == null || prevLeaf().isWhiteSpaceWithNewline20)

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

/**
 * Gets the max_line_length property if the `max-line-length` rule is enabled. Otherwise, returns [Int.MAX_VALUE].
 *
 * Normally, rules should not have direct dependencies on other rules. This rule is an exception to that. In case the `max-line-length`
 * property in the `.editorconfig` is set, or inferred via a default value based on the `ktlint_code_style`, but the `max-line-length` rule
 * is disabled, then those other rules might start wrapping lines. Conceptually, the `max-line-length` rule determines whether ktlint should
 * or should not use the `max_line_length` property.
 */
public fun EditorConfig.maxLineLength(): Int =
    if (maxLineLengthRuleEnabled()) {
        this[MAX_LINE_LENGTH_PROPERTY]
    } else {
        Int.MAX_VALUE
    }

private fun EditorConfig.maxLineLengthRuleEnabled(): Boolean =
    RuleExecution.enabled ==
        getEditorConfigValueOrNull(
            RULE_EXECUTION_PROPERTY_TYPE,
            MAX_LINE_LENGTH_RULE_ID.ktLintRuleExecutionPropertyName(),
        )

public val MAX_LINE_LENGTH_RULE_ID: RuleId = MaxLineLengthRule().ruleId
