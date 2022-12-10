package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Detects if given `ruleId` at given `offset` is suppressed.
 */
internal typealias SuppressionLocator = (offset: Int, ruleId: String, isRoot: Boolean) -> Boolean

internal object SuppressionLocatorBuilder {
    /**
     * No suppression is detected. Always returns `false`.
     */
    val NO_SUPPRESSION: SuppressionLocator = { _, _, _ -> false }

    /**
     * Mapping of non-ktlint annotations to ktlint-annotation so that ktlint rules will be suppressed automatically
     * when specific non-ktlint annotations are found. The prevents that developers have to specify multiple annotations
     * for the same violation.
     */
    private val SUPPRESS_ANNOTATION_RULE_MAP = mapOf(
        "EnumEntryName" to "enum-entry-name-case",
        "RemoveCurlyBracesFromTemplate" to "string-template",
    )
    private val SUPPRESS_ANNOTATIONS = setOf("Suppress", "SuppressWarnings")
    private const val SUPPRESS_ALL_KTLINT_RULES = "ktlint-all"

    private val COMMENT_REGEX = Regex("\\s")

    /**
     * Builds [SuppressionLocator] for given [rootNode] of AST tree.
     */
    fun buildSuppressedRegionsLocator(
        rootNode: ASTNode,
    ): SuppressionLocator {
        val hintsList = collect(rootNode)
        return if (hintsList.isEmpty()) {
            NO_SUPPRESSION
        } else {
            toSuppressedRegionsLocator(hintsList)
        }
    }

    private fun toSuppressedRegionsLocator(hintsList: List<SuppressionHint>): SuppressionLocator =
        { offset, ruleId, isRoot -> // TODO: Remove unused parameter isRoot
            hintsList
                .filter { offset in it.range }
                .any { hint -> hint.disabledRules.isEmpty() || hint.disabledRules.contains(ruleId) }
        }

    /**
     * @param range zero-based range of lines where lint errors should be suppressed
     * @param disabledRules empty set means "all"
     */
    private data class SuppressionHint(
        val range: IntRange,
        val disabledRules: Set<String> = emptySet(),
    )

    private fun collect(
        rootNode: ASTNode,
    ): List<SuppressionHint> {
        val result = ArrayList<SuppressionHint>()
        val open = ArrayList<SuppressionHint>()
        rootNode.collect { node ->
            if (node is PsiComment) {
                val text = node.getText()
                if (text.startsWith("//")) {
                    val commentText = text.removePrefix("//").trim()
                    parseHintArgs(commentText, "ktlint-disable")?.let { args ->
                        val lineStart = (
                            node.prevLeaf { it is PsiWhiteSpace && it.textContains('\n') } as PsiWhiteSpace?
                            )?.let { it.node.startOffset + it.text.lastIndexOf('\n') + 1 } ?: 0
                        result.add(SuppressionHint(IntRange(lineStart, node.startOffset), HashSet(args)))
                    }
                } else {
                    val commentText = text.removePrefix("/*").removeSuffix("*/").trim()
                    parseHintArgs(commentText, "ktlint-disable")?.apply {
                        open.add(SuppressionHint(IntRange(node.startOffset, node.startOffset), HashSet(this)))
                    }
                        ?: parseHintArgs(commentText, "ktlint-enable")?.apply {
                            // match open hint
                            val disabledRules = HashSet(this)
                            val openHintIndex = open.indexOfLast { it.disabledRules == disabledRules }
                            if (openHintIndex != -1) {
                                val openingHint = open.removeAt(openHintIndex)
                                result.add(
                                    SuppressionHint(
                                        IntRange(openingHint.range.first, node.startOffset - 1),
                                        disabledRules,
                                    ),
                                )
                            }
                        }
                }
            }
            // Extract all Suppress annotations and create SuppressionHints
            val psi = node.psi
            if (psi is KtAnnotated) {
                createSuppressionHintFromAnnotations(psi, SUPPRESS_ANNOTATIONS, SUPPRESS_ANNOTATION_RULE_MAP)
                    ?.let {
                        result.add(it)
                    }
            }
        }
        result.addAll(
            open.map {
                SuppressionHint(IntRange(it.range.first, rootNode.textLength), it.disabledRules)
            },
        )
        return result
    }

    private fun ASTNode.collect(block: (node: ASTNode) -> Unit) {
        block(this)
        this
            .getChildren(null)
            .forEach { it.collect(block) }
    }

    private fun parseHintArgs(
        commentText: String,
        key: String,
    ): List<String>? {
        if (commentText.startsWith(key)) {
            val parsedComment = splitCommentBySpace(commentText)
            // assert exact match
            if (parsedComment[0] == key) {
                return parsedComment.tail()
            }
        }
        return null
    }

    private fun splitCommentBySpace(
        comment: String,
    ) = comment
        .replace(COMMENT_REGEX, " ")
        .replace(" {2,}", " ")
        .split(" ")

    private fun <T> List<T>.tail() = this.subList(1, this.size)

    /**
     * Creates [SuppressionHint] from annotations of given [psi]
     * Returns null if no [targetAnnotations] present or no mapping exists
     * between annotations' values and ktlint rules
     */
    private fun createSuppressionHintFromAnnotations(
        psi: KtAnnotated,
        targetAnnotations: Collection<String>,
        annotationValueToRuleMapping: Map<String, String>,
    ): SuppressionHint? =
        psi
            .annotationEntries
            .filter {
                it.calleeExpression
                    ?.constructorReferenceExpression
                    ?.getReferencedName() in targetAnnotations
            }.flatMap(KtAnnotationEntry::getValueArguments)
            .mapNotNull { it.toRuleId(annotationValueToRuleMapping) }
            .let { suppressedRules ->
                when {
                    suppressedRules.isEmpty() -> null
                    suppressedRules.contains(SUPPRESS_ALL_KTLINT_RULES) ->
                        SuppressionHint(
                            IntRange(psi.startOffset, psi.endOffset),
                            emptySet(),
                        )
                    else ->
                        SuppressionHint(
                            IntRange(psi.startOffset, psi.endOffset),
                            suppressedRules.toSet(),
                        )
                }
            }

    private fun ValueArgument.toRuleId(annotationValueToRuleMapping: Map<String, String>): String? =
        getArgumentExpression()
            ?.text
            ?.removeSurrounding("\"")
            ?.let {
                when {
                    it == "ktlint" -> {
                        // Disable all rules
                        SUPPRESS_ALL_KTLINT_RULES
                    }
                    it.startsWith("ktlint:") -> {
                        // Disable specific rule
                        it.removePrefix("ktlint:")
                    }
                    else -> {
                        // Disable specific rule if it the annotion value is mapped to a specific rule
                        annotationValueToRuleMapping[it]
                    }
                }
            }
}
