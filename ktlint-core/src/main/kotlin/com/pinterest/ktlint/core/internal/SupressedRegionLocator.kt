package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Detects if given `ruleId` at given `offset` is suppressed.
 */
internal typealias SuppressionLocator = (offset: Int, ruleId: String, isRoot: Boolean) -> Boolean

/**
 * No suppression is detected. Always returns `false`.
 */
internal val noSuppression: SuppressionLocator = { _, _, _ -> false }

private val suppressAnnotationRuleMap = mapOf(
    "RemoveCurlyBracesFromTemplate" to "string-template"
)
private val suppressAnnotations = setOf("Suppress", "SuppressWarnings")
private val commentRegex = Regex("\\s")

/**
 * Builds [SuppressionLocator] for given [rootNode] of AST tree.
 */
internal fun buildSuppressedRegionsLocator(
    rootNode: ASTNode
): SuppressionLocator {
    val hintsList = collect(rootNode)
    return if (hintsList.isEmpty()) {
        noSuppression
    } else { offset, ruleId, isRoot ->
        if (isRoot) {
            val h = hintsList[0]
            h.range.last == 0 && (
                h.disabledRules.isEmpty() ||
                    h.disabledRules.contains(ruleId)
                )
        } else {
            hintsList.any { hint ->
                (
                    hint.disabledRules.isEmpty() ||
                        hint.disabledRules.contains(ruleId)
                    ) &&
                    hint.range.contains(offset)
            }
        }
    }
}

/**
 * @param range zero-based range of lines where lint errors should be suppressed
 * @param disabledRules empty set means "all"
 */
private data class SuppressionHint(
    val range: IntRange,
    val disabledRules: Set<String> = emptySet()
)

private fun collect(
    rootNode: ASTNode
): List<SuppressionHint> {
    val result = ArrayList<SuppressionHint>()
    val open = ArrayList<SuppressionHint>()
    rootNode.visit { node ->
        if (node is PsiComment) {
            val text = node.getText()
            if (text.startsWith("//")) {
                val commentText = text.removePrefix("//").trim()
                parseHintArgs(commentText, "ktlint-disable")?.let { args ->
                    val lineStart = (
                        node.prevLeaf { it is PsiWhiteSpace && it.textContains('\n') } as
                            PsiWhiteSpace?
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
                                    IntRange(openingHint.range.first, node.startOffset),
                                    disabledRules
                                )
                            )
                        }
                    }
            }
        }
        // Extract all Suppress annotations and create SuppressionHints
        val psi = node.psi
        if (psi is KtAnnotated) {
            createSuppressionHintFromAnnotations(psi, suppressAnnotations, suppressAnnotationRuleMap)
                ?.let {
                    result.add(it)
                }
        }
    }
    result.addAll(
        open.map {
            SuppressionHint(IntRange(it.range.first, rootNode.textLength), it.disabledRules)
        }
    )
    return result
}

private fun parseHintArgs(
    commentText: String,
    key: String
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
    comment: String
) = comment
    .replace(commentRegex, " ")
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
    annotationValueToRuleMapping: Map<String, String>
): SuppressionHint? = psi.annotationEntries
    .filter {
        it.calleeExpression
            ?.constructorReferenceExpression
            ?.getReferencedName() in targetAnnotations
    }
    .flatMap(KtAnnotationEntry::getValueArguments)
    .mapNotNull {
        it.getArgumentExpression()?.text?.removeSurrounding("\"")
    }
    .mapNotNull(annotationValueToRuleMapping::get)
    .let { suppressedRules ->
        if (suppressedRules.isNotEmpty()) {
            SuppressionHint(
                IntRange(psi.startOffset, psi.endOffset),
                suppressedRules.toSet()
            )
        } else {
            null
        }
    }
