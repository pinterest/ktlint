package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_END
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_START
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.EOL
import com.pinterest.ktlint.rule.engine.internal.rules.KTLINT_SUPPRESSION_RULE_ID
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
internal typealias SuppressionLocator = (offset: Int, ruleId: RuleId) -> Boolean

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
        { offset, ruleId ->
            if (ruleId == KTLINT_SUPPRESSION_RULE_ID) {
                // The rule to detect deprecated rule directives may not be disabled itself as otherwise the directives
                // will not be reported and fixed.
                false
            } else {
                hintsList
                    .filter { offset in it.range }
                    .any { hint -> hint.disabledRuleIds.isEmpty() || hint.disabledRuleIds.contains(ruleId.value) }
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
            commentSuppressionsHints.toSuppressionHints(rootNode),
        )
    }

    private fun ASTNode.collect(block: (node: ASTNode) -> Unit) {
        block(this)
        this
            .getChildren(null)
            .forEach { it.collect(block) }
    }

    private fun ASTNode.createSuppressionHintFromComment(formatterTags: FormatterTags): CommentSuppressionHint? =
        if (text.startsWith("//")) {
            createSuppressionHintFromEolComment(formatterTags)
        } else {
            createSuppressionHintFromBlockComment(formatterTags)
        }

    private fun ASTNode.createSuppressionHintFromEolComment(formatterTags: FormatterTags): CommentSuppressionHint? =
        text
            .removePrefix("//")
            .trim()
            .split(" ")
            .takeIf { it.isNotEmpty() }
            ?.takeIf { it[0] == formatterTags.formatterTagOff }
            ?.let { parts ->
                CommentSuppressionHint(
                    this,
                    HashSet(parts.tail()),
                    EOL,
                )
            }

    private fun ASTNode.createSuppressionHintFromBlockComment(formatterTags: FormatterTags): CommentSuppressionHint? =
        text
            .removePrefix("/*")
            .removeSuffix("*/")
            .trim()
            .split(" ")
            .takeIf { it.isNotEmpty() }
            ?.let { parts ->
                if (parts[0] == formatterTags.formatterTagOff) {
                    CommentSuppressionHint(
                        this,
                        HashSet(parts.tail()),
                        BLOCK_START,
                    )
                } else if (parts[0] == formatterTags.formatterTagOn) {
                    CommentSuppressionHint(
                        this,
                        HashSet(parts.tail()),
                        BLOCK_END,
                    )
                } else {
                    null
                }
            }

    private fun MutableList<CommentSuppressionHint>.toSuppressionHints(rootNode: ASTNode): MutableList<SuppressionHint> {
        val suppressionHints = mutableListOf<SuppressionHint>()
        val blockCommentSuppressionHints = mutableListOf<CommentSuppressionHint>()
        forEach { commentSuppressionHint ->
            when (commentSuppressionHint.type) {
                EOL -> {
                    val commentNode = commentSuppressionHint.node
                    suppressionHints.add(
                        SuppressionHint(
                            IntRange(commentNode.prevNewLineOffset(), commentNode.startOffset),
                            commentSuppressionHint.disabledRuleIds,
                        ),
                    )
                }

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
            blockCommentSuppressionHints.map {
                SuppressionHint(
                    IntRange(it.node.startOffset, rootNode.textLength),
                    it.disabledRuleIds,
                )
            },
        )
        return suppressionHints
    }

    private fun ASTNode.prevNewLineOffset(): Int =
        prevLeaf { it.isWhiteSpaceWithNewline() }
            ?.let { it.startOffset + it.text.lastIndexOf('\n') + 1 }
            ?: 0

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
            EOL,
            BLOCK_START,
            BLOCK_END,
        }
    }
}
