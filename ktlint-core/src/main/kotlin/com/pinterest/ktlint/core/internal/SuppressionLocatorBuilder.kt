package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Detects if given `ruleId` at given `offset` is suppressed.
 */
internal typealias SuppressionLocator = (offset: Int, ruleId: String) -> Boolean

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
    private val SUPPRESS_ANNOTATION_RULE_MAP = mapOf(
        "EnumEntryName" to "enum-entry-name-case",
        "RemoveCurlyBracesFromTemplate" to "string-template",
        "ClassName" to "experimental:class-naming",
        "FunctionName" to "experimental:function-naming",
        "PackageName" to "package-name",
        "PropertyName" to "experimental:property-naming",
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
        { offset, ruleId ->
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
            node
                .psi
                .createSuppressionHintFromAnnotations()
                ?.let {
                    result.add(it)
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
                .let { suppressedRules ->
                    when {
                        suppressedRules.isEmpty() -> null
                        suppressedRules.contains(SUPPRESS_ALL_KTLINT_RULES) ->
                            SuppressionHint(
                                IntRange(ktAnnotated.startOffset, ktAnnotated.endOffset),
                                emptySet(),
                            )

                        else ->
                            SuppressionHint(
                                IntRange(ktAnnotated.startOffset, ktAnnotated.endOffset),
                                suppressedRules.toSet(),
                            )
                    }
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
                        // Disable specific rule if the annotation value is mapped to a specific rule
                        annotationValueToRuleMapping[it]
                    }
                }
            }
}
