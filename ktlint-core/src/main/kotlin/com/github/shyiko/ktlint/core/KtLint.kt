package com.github.shyiko.ktlint.core

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.DefaultLogger
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions.getArea
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import sun.reflect.ReflectionFactory
import java.util.ArrayList
import java.util.HashSet
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.Logger as DiagnosticLogger

object KtLint {

    val EDITOR_CONFIG_USER_DATA_KEY = Key<EditorConfig>("EDITOR_CONFIG")
    val ANDROID_USER_DATA_KEY = Key<Boolean>("ANDROID")

    private val psiFileFactory: PsiFileFactory
    private val nullSuppression = { _: Int, _: String -> false }

    init {
        // do not print anything to the stderr when lexer is unable to match input
        class LoggerFactory : DiagnosticLogger.Factory {
            override fun getLoggerInstance(p: String): DiagnosticLogger = object : DefaultLogger(null) {
                override fun warn(message: String?, t: Throwable?) {}
                override fun error(message: String?, vararg details: String?) {}
            }
        }
        DiagnosticLogger.setFactory(LoggerFactory::class.java)
        val project = KotlinCoreEnvironment.createForProduction(Disposable {},
            CompilerConfiguration(), EnvironmentConfigFiles.EMPTY).project
        // everything below (up to PsiFileFactory.getInstance(...)) is to get AST mutations (`ktlint -F ...`) working
        // otherwise it's not needed
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
        }
        val extensionPoint = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
        val extensionClassName = TreeCopyHandler::class.java.name!!
        for (area in arrayOf(getArea(project), getArea(null))) {
            if (!area.hasExtensionPoint(extensionPoint)) {
                area.registerExtensionPoint(extensionPoint, extensionClassName, ExtensionPoint.Kind.INTERFACE)
            }
        }
        project as MockProject
        project.registerService(PomModel::class.java, pomModel)
        psiFileFactory = PsiFileFactory.getInstance(project)
    }

    /**
     * Check source for lint errors.
     *
     * @param text source
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param cb callback that is going to be executed for every lint error
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun lint(text: String, ruleSets: Iterable<RuleSet>, cb: (e: LintError) -> Unit) {
        lint(text, ruleSets, emptyMap(), cb, script = false)
    }

    fun lint(text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>, cb: (e: LintError) -> Unit) {
        lint(text, ruleSets, userData, cb, script = false)
    }

    /**
     * Check source for lint errors.
     *
     * @param text script source
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param cb callback that is going to be executed for every lint error
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun lintScript(text: String, ruleSets: Iterable<RuleSet>, cb: (e: LintError) -> Unit) {
        lint(text, ruleSets, emptyMap(), cb, script = true)
    }

    fun lintScript(text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>, cb: (e: LintError) -> Unit) {
        lint(text, ruleSets, userData, cb, script = true)
    }

    private fun lint(
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError) -> Unit,
        script: Boolean
    ) {
        val positionByOffset = calculateLineColByOffset(text).let {
            val offsetDueToLineBreakNormalization = calculateLineBreakOffset(text)
            return@let { offset: Int -> it(offset + offsetDueToLineBreakNormalization(offset)) }
        }
        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val fileName = if (script) "file.kts" else "file.kt"
        val psiFile = psiFileFactory.createFileFromText(fileName, KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = psiFile.findErrorElement()
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        rootNode.putUserData(EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(userData - "android"))
        rootNode.putUserData(ANDROID_USER_DATA_KEY, userData["android"]?.toBoolean() ?: false)
        val isSuppressed = calculateSuppressedRegions(rootNode)
        visitor(rootNode, ruleSets).invoke { node, rule, fqRuleId ->
            if (!isSuppressed(node.startOffset, fqRuleId)) {
                try {
                    rule.visit(node, false) { offset, errorMessage, _ ->
                        val (line, col) = positionByOffset(offset)
                        cb(LintError(line, col, fqRuleId, errorMessage))
                    }
                } catch (e: Exception) {
                    val (line, col) = positionByOffset(node.startOffset)
                    throw RuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }
    }

    private fun visitor(
        rootNode: ASTNode,
        ruleSets: Iterable<RuleSet>
    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val fqrsRestrictedToRoot = mutableListOf<Pair<String, Rule>>()
        val fqrs = mutableListOf<Pair<String, Rule>>()
        val fqrsExpectedToBeExecutedLast = mutableListOf<Pair<String, Rule>>()
        for (ruleSet in ruleSets) {
            val prefix = if (ruleSet.id === "standard") "" else "${ruleSet.id}:"
            for (rule in ruleSet) {
                val fqr = "$prefix${rule.id}" to rule
                when {
                    rule is Rule.Modifier.RestrictToRootLast -> fqrsExpectedToBeExecutedLast.add(fqr)
                    rule is Rule.Modifier.RestrictToRoot -> fqrsRestrictedToRoot.add(fqr)
                    else -> fqrs.add(fqr)
                }
            }
        }
        return { visit ->
            for ((fqRuleId, rule) in fqrsRestrictedToRoot) {
                visit(rootNode, rule, fqRuleId)
            }
            rootNode.visit { node ->
                for ((fqRuleId, rule) in fqrs) {
                    visit(node, rule, fqRuleId)
                }
            }
            for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLast) {
                visit(rootNode, rule, fqRuleId)
            }
        }
    }

    private fun calculateLineColByOffset(text: String): (offset: Int) -> Pair<Int, Int> {
        var i = -1
        val e = text.length
        val arr = ArrayList<Int>()
        do {
            arr.add(i + 1)
            i = text.indexOf('\n', i + 1)
        } while (i != -1)
        arr.add(e + if (arr.last() == e) 1 else 0)
        val segmentTree = SegmentTree(arr.toTypedArray())
        return { offset ->
            val line = segmentTree.indexOf(offset)
            if (line != -1) {
                val col = offset - segmentTree.get(line).left
                line + 1 to col + 1
            } else {
                1 to 1
            }
        }
    }

    private fun calculateSuppressedRegions(rootNode: ASTNode) =
        SuppressionHint.collect(rootNode).let { listOfHints ->
            if (listOfHints.isEmpty()) nullSuppression else { offset, ruleId ->
                listOfHints.any { (range, disabledRules) ->
                    (disabledRules.isEmpty() || disabledRules.contains(ruleId)) && range.contains(offset) }
            }
        }

    /**
     * Fix style violations.
     *
     * @param text source
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param cb callback that is going to be executed for every lint error
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun format(text: String, ruleSets: Iterable<RuleSet>, cb: (e: LintError, corrected: Boolean) -> Unit): String =
        format(text, ruleSets, emptyMap<String, String>(), cb, script = false)

    fun format(text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
        cb: (e: LintError, corrected: Boolean) -> Unit): String = format(text, ruleSets, userData, cb, script = false)

    /**
     * Fix style violations.
     *
     * @param text script source
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param cb callback that is going to be executed for every lint error
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun formatScript(text: String, ruleSets: Iterable<RuleSet>, cb: (e: LintError, corrected: Boolean) -> Unit): String =
        format(text, ruleSets, emptyMap(), cb, script = true)

    fun formatScript(text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
        cb: (e: LintError, corrected: Boolean) -> Unit): String = format(text, ruleSets, userData, cb, script = true)

    private fun format(
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError, corrected: Boolean) -> Unit,
        script: Boolean
    ): String {
        val positionByOffset = calculateLineColByOffset(text).let {
            val offsetDueToLineBreakNormalization = calculateLineBreakOffset(text)
            return@let { offset: Int -> it(offset + offsetDueToLineBreakNormalization(offset)) }
        }
        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val fileName = if (script) "file.kts" else "file.kt"
        val psiFile = psiFileFactory.createFileFromText(fileName, KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = psiFile.findErrorElement()
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        rootNode.putUserData(EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(userData - "android"))
        rootNode.putUserData(ANDROID_USER_DATA_KEY, userData["android"]?.toBoolean() ?: false)
        var isSuppressed = calculateSuppressedRegions(rootNode)
        val visit = visitor(rootNode, ruleSets)
        var autoCorrect = false
        visit { node, rule, fqRuleId ->
            if (!isSuppressed(node.startOffset, fqRuleId)) {
                try {
                    rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                        if (canBeAutoCorrected) {
                            autoCorrect = true
                        }
                        val (line, col) = positionByOffset(offset)
                        cb(LintError(line, col, fqRuleId, errorMessage), canBeAutoCorrected)
                    }
                } catch (e: Exception) {
                    val (line, col) = positionByOffset(node.startOffset)
                    throw RuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }
        if (autoCorrect) {
            visit { node, rule, fqRuleId ->
                if (!isSuppressed(node.startOffset, fqRuleId)) {
                    try {
                        rule.visit(node, true) { _, _, canBeAutoCorrected ->
                            if (canBeAutoCorrected && isSuppressed !== nullSuppression) {
                                isSuppressed = calculateSuppressedRegions(rootNode)
                            }
                        }
                    } catch (e: Exception) {
                        // line/col cannot be reliably mapped as exception might originate from a node not present
                        // in the original AST
                        throw RuleExecutionException(0, 0, fqRuleId, e)
                    }
                }
            }
            return rootNode.text.replace("\n", determineLineSeparator(text))
        }
        return text
    }

    private fun calculateLineBreakOffset(fileContent: String): (offset: Int) -> Int {
        val arr = ArrayList<Int>()
        var i = 0
        do {
            arr.add(i)
            i = fileContent.indexOf("\r\n", i + 1)
        } while (i != -1)
        arr.add(fileContent.length)
        return if (arr.size != 2)
            SegmentTree(arr.toTypedArray()).let { return { offset -> it.indexOf(offset) } } else { _ -> 0 }
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
                            }
                            ?: parseHintArgs(commentText, "ktlint-enable")?.apply {
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

            private fun <T> List<T>.tail() = this.subList(1, this.size)
        }
    }

    private class SegmentTree {

        private val segments: List<Segment>

        constructor(sortedArray: Array<Int>) {
            require(sortedArray.size > 1) { "At least two data points are required" }
            sortedArray.reduce { r, v -> require(r <= v) { "Data points are not sorted (ASC)" }; v }
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
    }

    private data class Segment(val left: Int, val right: Int)

    private fun PsiElement.findErrorElement(): PsiErrorElement? {
        if (this is PsiErrorElement) {
            return this
        }
        this.children.forEach { child ->
            val errorElement = child.findErrorElement()
            if (errorElement != null) {
                return errorElement
            }
        }
        return null
    }

    private fun ASTNode.visit(cb: (node: ASTNode) -> Unit) {
        cb(this)
        this.getChildren(null).forEach { it.visit(cb) }
    }
}
