package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_END
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.BLOCK_START
import com.pinterest.ktlint.rule.engine.internal.SuppressionLocatorBuilder.CommentSuppressionHint.Type.EOL
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
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

    private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

    /**
     * Mapping of non-ktlint annotations to ktlint-annotation so that ktlint rules will be suppressed automatically
     * when specific non-ktlint annotations are found. The prevents that developers have to specify multiple annotations
     * for the same violation.
     */
    private val SUPPRESS_ANNOTATION_RULE_MAP =
        mapOf(
            // It would have been nice if the official rule id's as defined in the Rules themselves could have been used here. But that would
            // introduce a circular dependency between the ktlint-rule-engine and the ktlint-ruleset-standard modules.
            "EnumEntryName" to RuleId("standard:enum-entry-name-case"),
            "RemoveCurlyBracesFromTemplate" to RuleId("standard:string-template"),
            "ClassName" to RuleId("standard:class-naming"),
            "FunctionName" to RuleId("standard:function-naming"),
            "PackageName" to RuleId("standard:package-name"),
            "PropertyName" to RuleId("standard:property-naming"),
        )
    private val SUPPRESS_ANNOTATIONS = setOf("Suppress", "SuppressWarnings")
    private val SUPPRESS_ALL_KTLINT_RULES_RULE_ID = RuleId("ktlint:suppress-all-rules")
    private val KTLINT_SUPPRESSION_RULE_ID = RuleId("standard:ktlint-suppression")

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
                    .any { hint -> hint.disabledRuleIds.isEmpty() || hint.disabledRuleIds.contains(ruleId) }
            }
        }

    private fun collect(
        rootNode: ASTNode,
        formatterTags: FormatterTags,
    ): List<SuppressionHint> {
        val suppressionHints = ArrayList<SuppressionHint>()
        val commentSuppressionsHints = mutableListOf<CommentSuppressionHint>()
        rootNode.collect { node ->
            node
                .takeIf { it is PsiComment }
                ?.createSuppressionHintFromComment(formatterTags)
                ?.let { commentSuppressionsHints.add(it) }

            // Extract all Suppress annotations and create SuppressionHints
            node
                .psi
                .safeAs<KtAnnotated>()
                ?.createSuppressionHintFromAnnotations()
                ?.let { suppressionHints.add(it) }
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
            ?.takeIf { it[0] == KTLINT_DISABLE || it[0] == formatterTags.formatterTagOff }
            ?.let { parts ->
                CommentSuppressionHint(
                    this,
                    HashSet(parts.tailToRuleIds()),
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
                if (parts[0] == KTLINT_DISABLE || parts[0] == formatterTags.formatterTagOff) {
                    CommentSuppressionHint(
                        this,
                        HashSet(parts.tailToRuleIds()),
                        BLOCK_START,
                    )
                } else if (parts[0] == KTLINT_ENABLE || parts[0] == formatterTags.formatterTagOn) {
                    CommentSuppressionHint(
                        this,
                        HashSet(parts.tailToRuleIds()),
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
                    Unit
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

    private fun List<String>.tailToRuleIds() = tail().mapNotNull { createRuleIdOrNull(it) }

    private fun <T> List<T>.tail() = this.subList(1, this.size)

    /**
     * Creates [SuppressionHint] from annotations of given [PsiElement]. Returns null if no targetAnnotations are
     * present or no mapping exists between annotations' values and ktlint rules
     */
    private fun PsiElement.createSuppressionHintFromAnnotations(): SuppressionHint? =
        (this as? KtAnnotated)?.let { ktAnnotated ->
            ktAnnotated
                .annotationEntries
                .filter {
                    it.calleeExpression
                        ?.constructorReferenceExpression
                        ?.getReferencedName() in SUPPRESS_ANNOTATIONS
                }.flatMap(KtAnnotationEntry::getValueArguments)
                .mapNotNull { it.toRuleId(SUPPRESS_ANNOTATION_RULE_MAP) }
                .let { suppressedRuleIds ->
                    when {
                        suppressedRuleIds.isEmpty() -> null
                        suppressedRuleIds.contains(SUPPRESS_ALL_KTLINT_RULES_RULE_ID) ->
                            SuppressionHint(
                                IntRange(ktAnnotated.startOffset, ktAnnotated.endOffset),
                                emptySet(),
                            )

                        else ->
                            SuppressionHint(
                                IntRange(ktAnnotated.startOffset, ktAnnotated.endOffset),
                                suppressedRuleIds.toSet(),
                            )
                    }
                }
        }

    private fun ValueArgument.toRuleId(annotationValueToRuleMapping: Map<String, RuleId>): RuleId? =
        getArgumentExpression()
            ?.text
            ?.removeSurrounding("\"")
            ?.let { argumentExpressionText ->
                when {
                    argumentExpressionText == "ktlint" -> {
                        // Disable all rules
                        SUPPRESS_ALL_KTLINT_RULES_RULE_ID
                    }
                    argumentExpressionText.startsWith("ktlint:") -> {
                        // Disable specific rule
                        argumentExpressionText
                            .removePrefix("ktlint:")
                            .let { createRuleIdOrNull(it) }
                    }
                    else -> {
                        // Disable specific rule if the annotation value is mapped to a specific rule
                        annotationValueToRuleMapping[argumentExpressionText]
                    }
                }
            }

    private fun createRuleIdOrNull(ruleId: String): RuleId? =
        try {
            // For backwards compatibility the suppression hints have to be prefixed with the standard rule set id when the rule id is
            // not prefixed with any rule set id.
            RuleId
                .prefixWithStandardRuleSetIdWhenMissing(ruleId)
                .let { RuleId(it) }
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Ktlint should not terminate with an exception in case the code being scanned contains a suppression for a non-existing rule.
            // Instead, a warning should be printed and the invalid reference is to be ignored. The original ruleId is printed in the
            // warning message so that user will not go searching for the fully qualified rule id while the code actually contained an
            // unqualified ruleId.
            LOGGER.warn {
                """
                Can not suppress rule with id '$ruleId'. Please check and fix references to this rule in your code.
                    Underlying cause: ${illegalArgumentException.message}
                """.trimIndent()
            }
            null
        }

    /**
     * @param range zero-based range of lines where lint errors should be suppressed
     * @param disabledRuleIds empty set means "all"
     */
    private data class SuppressionHint(
        val range: IntRange,
        val disabledRuleIds: Set<RuleId> = emptySet(),
    )

    private data class CommentSuppressionHint(
        val node: ASTNode,
        val disabledRuleIds: Set<RuleId> = emptySet(),
        val type: Type,
    ) {
        enum class Type {
            EOL,
            BLOCK_START,
            BLOCK_END,
        }
    }

    private const val KTLINT_DISABLE = "ktlint-disable"
    private const val KTLINT_ENABLE = "ktlint-enable"
}
