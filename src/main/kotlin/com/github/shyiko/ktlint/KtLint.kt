package com.github.shyiko.ktlint

import com.github.shyiko.ktlint.rule.IndentationRule
import com.github.shyiko.ktlint.rule.NoConsecutiveBlankLinesRule
import com.github.shyiko.ktlint.rule.NoMultipleSpacesRule
import com.github.shyiko.ktlint.rule.NoSemicolonsRule
import com.github.shyiko.ktlint.rule.NoTrailingSpacesRule
import com.github.shyiko.ktlint.rule.NoUnusedImportsRule
import com.github.shyiko.ktlint.rule.NoWildcardImportsRule
import com.github.shyiko.ktlint.rule.Rule
import com.github.shyiko.ktlint.rule.SpacingAfterCommaRule
import com.github.shyiko.ktlint.rule.SpacingAfterKeywordRule
import com.github.shyiko.ktlint.rule.SpacingAroundColonRule
import com.github.shyiko.ktlint.rule.SpacingAroundCurlyRule
import com.github.shyiko.ktlint.rule.SpacingAroundOperatorsRule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.event.PomModelListener
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import sun.reflect.ReflectionFactory
import java.util.ArrayList
import java.util.HashSet

object KtLint {

    private val psiFileFactory: PsiFileFactory

    init {
        val project = KotlinCoreEnvironment.createForProduction(Disposable {},
            CompilerConfiguration(), emptyList()).project
        // everything below (up until PsiFileFactory.getInstance(...) is to get AST mutations working
        // (required by `format`)
        val pomModel: PomModel = object : UserDataHolderBase(), PomModel {

            override fun runTransaction(transaction: PomTransaction) {
                (transaction as PomTransactionBase).run()
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T : PomModelAspect> getModelAspect(aspect: Class<T>): T? {
                if (aspect == TreeAspect::class.java) {
                    // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
                    // (check constructor signature and compare it to the source)
                    // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
                    val constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(
                        aspect, Any::class.java.getDeclaredConstructor(*arrayOfNulls<Class<*>>(0)))
                    return constructor.newInstance(*emptyArray()) as T
                }
                return null
            }

            override fun addModelListener(listener: PomModelListener) {}
        }
        Extensions.getArea(project).registerExtensionPoint("org.jetbrains.kotlin.com.intellij.treeCopyHandler",
            "org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler", ExtensionPoint.Kind.INTERFACE)
        val defaultArea = Extensions.getArea(null)
        if (!defaultArea.hasExtensionPoint("org.jetbrains.kotlin.com.intellij.treeCopyHandler")) {
            defaultArea.registerExtensionPoint("org.jetbrains.kotlin.com.intellij.treeCopyHandler",
                "org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler",
                ExtensionPoint.Kind.INTERFACE)
        }
        (project as MockProject).registerService(PomModel::class.java, pomModel)
        psiFileFactory = PsiFileFactory.getInstance(project)
    }

    /**
     * @throws ParseException
     * @throws RuleExecutionException
     */
    fun lint(text: String, cb: (e: LintError) -> Unit) = lint(text, cb, defaultRules())

    internal fun lint(text: String, cb: (e: LintError) -> Unit, rules: Array<Pair<String, Rule>>) {
        val positionByOffset = constructPositionByOffset(text).let {
            val offsetDueToLineBreakNormalization = calculateLineBreakOffset(text)
            return@let { offset: Int -> it(offset + offsetDueToLineBreakNormalization(offset)) }
        }
        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val psiFile = psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = findErrorElement(psiFile)
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        val suppressedAt = constructSuppressedAt(rootNode)
        rootNode.visit { node ->
            rules.forEach {
                val (id, rule) = it
                if (!suppressedAt(node.startOffset, id)) {
                    try {
                        rule.visit(node, false) {
                            val (line, col) = positionByOffset(it.offset)
                            cb(LintError(line, col, id, it.detail))
                        }
                    } catch (e: Exception) {
                        val (line, col) = positionByOffset(node.startOffset)
                        throw RuleExecutionException(line, col, id, e)
                    }
                }
            }
        }
    }

    private fun constructPositionByOffset(text: String): (offset: Int) -> Pair<Int, Int> {
        var i = 0
        val e = text.length
        val arr = ArrayList<Int>()
        do {
            arr.add(i)
            i = text.indexOf('\n', i) + 1
        } while (i != 0 && i != e)
        arr.add(e)
        val segmentTree = SegmentTree(arr.toTypedArray())
        return { offset ->
            val line = segmentTree.indexOf(offset)
            val col = offset - segmentTree.get(line).left
            line + 1 to col + 1
        }
    }

    private fun constructSuppressedAt(rootNode: ASTNode) =
        SuppressionHint.collect(rootNode).let {
            if (it.isEmpty()) { offset: Int, ruleId: String -> false } else { offset, ruleId ->
                it.any { (it.disabledRules.isEmpty() || it.disabledRules.contains(ruleId)) &&
                    it.range.contains(offset) }
            }
        }

    private fun defaultRules(): Array<Pair<String, Rule>> = arrayOf(
        "indent" to IndentationRule(),
        "no-consecutive-blank-lines" to NoConsecutiveBlankLinesRule(),
        "no-multi-spaces" to NoMultipleSpacesRule(),
        "no-semi" to NoSemicolonsRule(),
        "no-trailing-spaces" to NoTrailingSpacesRule(),
        "no-unused-imports" to NoUnusedImportsRule(),
        "no-wildcard-imports" to NoWildcardImportsRule(),
        "comma-spacing" to SpacingAfterCommaRule(),
        "keyword-spacing" to SpacingAfterKeywordRule(),
        "colon-spacing" to SpacingAroundColonRule(),
        "curly-spacing" to SpacingAroundCurlyRule(),
        "op-spacing" to SpacingAroundOperatorsRule()
    )

    /**
     * @throws ParseException
     * @throws RuleExecutionException
     */
    fun format(text: String): String = format(text, defaultRules())

    fun format(text: String, rules: Array<Pair<String, Rule>>): String {
        val positionByOffset = constructPositionByOffset(text).let {
            val offsetDueToLineBreakNormalization = calculateLineBreakOffset(text)
            return@let { offset: Int -> it(offset + offsetDueToLineBreakNormalization(offset)) }
        }
        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val psiFile = psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = findErrorElement(psiFile)
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        val suppressedAt = constructSuppressedAt(rootNode)
        var mutated: Boolean = false
        rootNode.visit { node ->
            rules.forEach {
                val (id, rule) = it
                if (!suppressedAt(node.startOffset, id)) {
                    try {
                        rule.visit(node, true) {
                            if (it.corrected) {
                                mutated = true
                            }
                        }
                    } catch (e: Exception) {
                        val (line, col) = positionByOffset(node.startOffset)
                        throw RuleExecutionException(line, col, id, e)
                    }
                }
            }
        }
        return if (mutated) rootNode.text.replace("\n", determineLineSeparator(text)) else text
    }

    private fun calculateLineBreakOffset(fileContent: String): (offset: Int) -> Int {
        val arr = ArrayList<Int>()
        var i: Int = 0
        do {
            arr.add(i)
            i = fileContent.indexOf("\r\n", i + 1)
        } while (i != -1)
        arr.add(fileContent.length)
        return if (arr.size != 2)
            SegmentTree(arr.toTypedArray()).let { return { offset -> it.indexOf(offset) } } else { offset -> 0 }
    }

    private fun determineLineSeparator(fileContent: String): String {
        val i = fileContent.lastIndexOf('\n')
        if (i == -1) {
            return if (fileContent.lastIndexOf('\r') == -1) System.getProperty("line.separator") else "\r"
        }
        return if (i != 0 && fileContent[i] == '\r') "\r\n" else "\n"
    }

    /**
     * @param range zero-based range of lines where lint errors should be suppressed
     * @param disabledRules empty set means "all"
     */
    private data class SuppressionHint(val range: IntRange, val disabledRules: Set<String> = emptySet()) {

        companion object {

            fun collect(rootNode: ASTNode): List<SuppressionHint> {
                val result = ArrayList<SuppressionHint>()
                val open = ArrayList<SuppressionHint>()
                rootNode.visit { node ->
                    if (node is PsiComment) {
                        val text = node.getText()
                        if (text.startsWith("//")) {
                            val commentText = text.removePrefix("//").trim()
                            parseHintArgs(commentText, "ktlint-disable")?.let { args ->
                                val lineStart = (node.prevLeaf { it is PsiWhiteSpace && it.textContains('\n') } as
                                    PsiWhiteSpace?)?.let { it.startOffset + it.text.lastIndexOf('\n') + 1 } ?: 0
                                result.add(SuppressionHint(IntRange(lineStart, node.startOffset), HashSet(args)))
                            }
                        } else {
                            val commentText = text.removePrefix("/*").removeSuffix("*/").trim()
                            parseHintArgs(commentText, "ktlint-disable")?.apply {
                                open.add(SuppressionHint(IntRange(node.startOffset, node.startOffset), HashSet(this)))
                            } ?:
                                parseHintArgs(commentText, "ktlint-enable")?.apply {
                                    // match open hint
                                    val disabledRules = HashSet(this)
                                    val openHintIndex = open.indexOfLast { it.disabledRules == disabledRules }
                                    if (openHintIndex != -1) {
                                        val openingHint = open.removeAt(openHintIndex)
                                        result.add(SuppressionHint(IntRange(openingHint.range.start, node.startOffset),
                                            disabledRules))
                                    }
                                }
                        }
                    }
                }
                result.addAll(open.map {
                    SuppressionHint(IntRange(it.range.first, rootNode.textLength), it.disabledRules)
                })
                return result
            }

            private fun parseHintArgs(commentText: String, key: String): List<String>? {
                if (commentText.startsWith(key)) {
                    val parsedComment = splitCommentBySpace(commentText)
                    // assert exact match
                    if (parsedComment[0] == key) {
                        return parsedComment.tail()
                    }
                }
                return null
            }

            private fun splitCommentBySpace(comment: String) =
                comment.replace(Regex("\\s"), " ").replace(" {2,}", " ").split(" ")

        }

    }

}

/**
 * Simplest segment tree where no intervals overlap.
 */
class SegmentTree {

    private val segments: List<Segment>
    val size: Int
        get() = segments.size

    constructor(sortedArray: Array<Int>) {
        require(sortedArray.size > 1, { "At least two data points are required" })
        sortedArray.reduce({ r, v -> require(r <= v, { "Data points are not sorted (ASC)" }); v })
        segments = sortedArray.take(sortedArray.size - 1)
            .mapIndexed { i: Int, v: Int -> Segment(v, sortedArray[i + 1] - 1) }
    }

    fun get(i: Int): Segment = segments[i]
    fun indexOf(v: Int): Int = binarySearch(v, 0, this.segments.size - 1)

    private fun binarySearch(v: Int, l: Int, r: Int): Int = when {
        l > r -> -1
        else -> {
            val i = l + (r - l) / 2
            val s = segments[i]
            if (v < s.left) binarySearch(v, l, i - 1) else (if (s.right < v) binarySearch(v, i + 1, r) else i)
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other?.javaClass == javaClass && segments == (other as SegmentTree).segments)

    override fun hashCode(): Int = segments.hashCode()

}

data class Segment(val left: Int, val right: Int)

class ParseException(val line: Int, val col: Int, msg: String) : Exception(msg)
class RuleExecutionException(val line: Int, val col: Int, val ruleId: String, cause: Throwable) : Exception(cause)

data class LintError(val line: Int, val col: Int, val id: String, val detail: String)
