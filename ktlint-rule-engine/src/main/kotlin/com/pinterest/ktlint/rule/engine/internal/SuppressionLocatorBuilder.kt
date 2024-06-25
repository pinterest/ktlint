package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.IgnoreKtlintSuppressions
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_END
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_START
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Detects if given `ruleId` at given `offset` is suppressed.
 */
internal typealias SuppressionLocator = (offset: Int, rule: Rule) -> Boolean

internal object SuppressionLocatorBuilder {
    /**
     * No suppression is detected. Always returns `false`.
     */
    private val NO_SUPPRESSION: SuppressionLocator = { _, _ -> false }

    /**
     * Mapping of non-ktlint annotations to ktlint-annotation so that ktlint rules will be suppressed automatically
     * when specific non-ktlint annotations are found. The prevents that developers have to specify multiple annotations
     * for the same violation.
     */
    private val SUPPRESS_ANNOTATION_RULE_MAP =
        mapOf(
            // It would have been nice if the official rule id's as defined in the Rules themselves could have been used here. But that
            // would introduce a circular dependency between the ktlint-rule-engine and the ktlint-ruleset-standard modules.
            "EnumEntryName" to "standard:enum-entry-name-case",
            "RemoveCurlyBracesFromTemplate" to "standard:string-template",
            "ClassName" to "standard:class-naming",
            "FunctionName" to "standard:function-naming",
            "PackageName" to "standard:package-name",
            "PropertyName" to "standard:property-naming",
            "ConstPropertyName" to "standard:property-naming",
            "ObjectPropertyName" to "standard:property-naming",
            "PrivatePropertyName" to "standard:property-naming",
            "UnusedImport" to "standard:no-unused-imports",
        )
    private val SUPPRESS_ANNOTATIONS = setOf("Suppress", "SuppressWarnings")
    private const val ALL_KTLINT_RULES_SUPPRESSION_ID = "ktlint:suppress-all-rules"

    /**
     * Builds [SuppressionLocator] for given [rootNode] of AST tree.
     */
    fun buildSuppressedRegionsLocator(
        rootNode: ASTNode,
        editorConfig: EditorConfig,
    ): SuppressionLocator {
        val hintsList = collect(rootNode, FormatterTags.from(editorConfig))
        return if (hintsList.isEmpty()) {
            NO_SUPPRESSION
        } else {
            toSuppressedRegionsLocator(hintsList)
        }
    }

    private fun toSuppressedRegionsLocator(hintsList: List<SuppressionHint>): SuppressionLocator =
        { offset, rule ->
            if (rule is IgnoreKtlintSuppressions) {
                false
            } else {
                hintsList
                    .filter { offset in it.range }
                    .any { hint -> hint.disabledRuleIds.isEmpty() || hint.disabledRuleIds.contains(rule.ruleId.value) }
            }
        }

    private fun collect(
        rootNode: ASTNode,
        formatterTags: FormatterTags,
    ): List<SuppressionHint> {
        val suppressionHints = ArrayList<SuppressionHint>()
        val commentSuppressionsHints = mutableListOf<CommentSuppressionHint>()
        rootNode.collect { node ->
            when (val psi = node.psi) {
                is PsiComment ->
                    node
                        .createSuppressionHintFromComment(formatterTags)
                        ?.let { commentSuppressionsHints.add(it) }

                is KtAnnotated ->
                    psi
                        .createSuppressionHintFromAnnotations()
                        ?.let { suppressionHints.add(it) }
            }
        }

        return suppressionHints.plus(
            commentSuppressionsHints.toSuppressionHints(),
        )
    }

    private fun ASTNode.collect(block: (node: ASTNode) -> Unit) {
        block(this)
        this
            .getChildren(null)
            .forEach { it.collect(block) }
    }

    private fun ASTNode.createSuppressionHintFromComment(formatterTags: FormatterTags): CommentSuppressionHint? =
        text
            .removePrefix("//")
            .removePrefix("/*")
            .removeSuffix("*/")
            .trim()
            .split(" ")
            .takeIf { it.isNotEmpty() }
            ?.let { parts ->
                when (parts[0]) {
                    formatterTags.formatterTagOff ->
                        CommentSuppressionHint(
                            this,
                            HashSet(parts.tail()),
                            BLOCK_START,
                        )

                    formatterTags.formatterTagOn ->
                        CommentSuppressionHint(
                            this,
                            HashSet(parts.tail()),
                            BLOCK_END,
                        )

                    else ->
                        null
                }
            }

    private fun MutableList<CommentSuppressionHint>.toSuppressionHints(): MutableList<SuppressionHint> {
        val suppressionHints = mutableListOf<SuppressionHint>()
        val blockCommentSuppressionHints = mutableListOf<CommentSuppressionHint>()
        forEach { commentSuppressionHint ->
            when (commentSuppressionHint.type) {
                BLOCK_START -> {
                    blockCommentSuppressionHints.add(commentSuppressionHint)
                }

                BLOCK_END -> {
                    // match open hint
                    blockCommentSuppressionHints
                        .lastOrNull { it.disabledRuleIds == commentSuppressionHint.disabledRuleIds }
                        ?.let { openHint ->
                            blockCommentSuppressionHints.remove(openHint)
                            suppressionHints.add(
                                SuppressionHint(
                                    IntRange(openHint.node.startOffset, commentSuppressionHint.node.startOffset - 1),
                                    commentSuppressionHint.disabledRuleIds,
                                ),
                            )
                        }
                }
            }
        }
        suppressionHints.addAll(
            // Remaining hints were not properly closed
            blockCommentSuppressionHints.map {
                val rbraceOfContainingBlock = it.node.rbraceOfContainingBlock()
                if (rbraceOfContainingBlock == null) {
                    // Apply suppression on next sibling only, when the outer element does not end with a RBRACE
                    it.node
                        .nextSibling()
                        .let { nextSibling ->
                            SuppressionHint(
                                IntRange(
                                    it.node.startOffset,
                                    nextSibling?.textRange?.endOffset ?: it.node.textRange.endOffset,
                                ),
                                it.disabledRuleIds,
                            )
                        }
                } else {
                    // Exclude closing curly brace of the containing block
                    SuppressionHint(
                        IntRange(it.node.startOffset, rbraceOfContainingBlock.startOffset - 1),
                        it.disabledRuleIds,
                    )
                }
            },
        )
        return suppressionHints
    }

    private fun ASTNode.rbraceOfContainingBlock(): ASTNode? =
        treeParent
            .lastChildNode
            ?.takeIf { it.elementType == RBRACE }

    private fun <T> List<T>.tail() = this.subList(1, this.size)

    private fun KtAnnotated.createSuppressionHintFromAnnotations(): SuppressionHint? =
        annotationEntries
            .filter {
                it
                    .calleeExpression
                    ?.constructorReferenceExpression
                    ?.getReferencedName() in SUPPRESS_ANNOTATIONS
            }.flatMap(KtAnnotationEntry::getValueArguments)
            .mapNotNull { it.toRuleId(SUPPRESS_ANNOTATION_RULE_MAP) }
            .let { suppressedRuleIds ->
                when {
                    suppressedRuleIds.isEmpty() -> null

                    suppressedRuleIds.contains(ALL_KTLINT_RULES_SUPPRESSION_ID) ->
                        SuppressionHint(
                            IntRange(startOffset, endOffset - 1),
                            emptySet(),
                        )

                    else ->
                        SuppressionHint(
                            IntRange(startOffset, endOffset - 1),
                            suppressedRuleIds.toSet(),
                        )
                }
            }

    private fun ValueArgument.toRuleId(annotationValueToRuleMapping: Map<String, String>): String? =
        getArgumentExpression()
            ?.text
            ?.removeSurrounding("\"")
            ?.let { argumentExpressionText ->
                when {
                    argumentExpressionText == "ktlint" -> {
                        // Disable all rules
                        ALL_KTLINT_RULES_SUPPRESSION_ID
                    }

                    argumentExpressionText.startsWith("ktlint:") -> {
                        // Disable specific rule. For backwards compatibility prefix rules without rule set id with the "standard" rule set
                        // id. Note that the KtlintSuppressionRule will emit a lint violation on the id. So this fix is only applicable for
                        // code bases in which the rule and suppression id's have not yet been fixed.
                        argumentExpressionText
                            .removePrefix("ktlint:")
                            .let { RuleId.prefixWithStandardRuleSetIdWhenMissing(it) }
                    }

                    else -> {
                        // Disable specific rule if the annotation value is mapped to a specific rule
                        annotationValueToRuleMapping[argumentExpressionText]
                    }
                }
            }

    /**
     * @param range zero-based range of lines where lint errors should be suppressed
     * @param disabledRuleIds empty set means "all"
     */
    private data class SuppressionHint(
        val range: IntRange,
        val disabledRuleIds: Set<String> = emptySet(),
    )

    private data class CommentSuppressionHint(
        val node: ASTNode,
        val disabledRuleIds: Set<String> = emptySet(),
        val type: Type,
    ) {
        enum class Type {
            BLOCK_START,
            BLOCK_END,
        }
    }
}
