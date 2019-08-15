package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.internal.EditorConfigInternal
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.DefaultLogger
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.Logger as DiagnosticLogger
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
import sun.reflect.ReflectionFactory

object KtLint {

    val EDITOR_CONFIG_USER_DATA_KEY = Key<EditorConfig>("EDITOR_CONFIG")
    val ANDROID_USER_DATA_KEY = Key<Boolean>("ANDROID")
    val FILE_PATH_USER_DATA_KEY = Key<String>("FILE_PATH")
    val DISABLED_RULES = Key<Set<String>>("DISABLED_RULES")
    const val STDIN_FILE = "<stdin>"

    private val psiFileFactory: PsiFileFactory
    private val nullSuppression = { _: Int, _: String, _: Boolean -> false }

    /**
     * @param fileName path of file to lint/format
     * @param text Contents of file to lint/format
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param userData Map of user options
     * @param cb callback invoked for each lint error
     * @param script true if this is a Kotlin script file
     * @param editorConfigPath optional path of the .editorconfig file (otherwise will use working directory)
     * @param debug True if invoked with the --debug flag
     */
    data class Params(
        val fileName: String? = null,
        val text: String,
        val ruleSets: Iterable<RuleSet>,
        val userData: Map<String, String> = emptyMap(),
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val editorConfigPath: String? = null,
        val debug: Boolean = false
    )

    init {
        // do not print anything to the stderr when lexer is unable to match input
        class LoggerFactory : DiagnosticLogger.Factory {
            override fun getLoggerInstance(p: String): DiagnosticLogger = object : DefaultLogger(null) {
                override fun warn(message: String?, t: Throwable?) {}
                override fun error(message: String?, vararg details: String?) {}
            }
        }
        DiagnosticLogger.setFactory(LoggerFactory::class.java)
        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val project = KotlinCoreEnvironment.createForProduction(
            Disposable {},
            compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
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
                        aspect, Any::class.java.getDeclaredConstructor(*arrayOfNulls<Class<*>>(0))
                    )
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
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun lint(params: Params) {
        val normalizedText = params.text.replace("\r\n", "\n").replace("\r", "\n")
        val positionByOffset = calculateLineColByOffset(normalizedText)
        val psiFileName = if (params.script) "file.kts" else "file.kt"
        val psiFile = psiFileFactory.createFileFromText(psiFileName, KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = psiFile.findErrorElement()
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        // Passed-in userData overrides .editorconfig
        val mergedUserData = userDataResolver(params.editorConfigPath, params.debug)(params.fileName) + params.userData
        injectUserData(rootNode, mergedUserData)
        val isSuppressed = calculateSuppressedRegions(rootNode)
        val errors = mutableListOf<LintError>()
        visitor(rootNode, params.ruleSets).invoke { node, rule, fqRuleId ->
            // fixme: enforcing suppression based on node.startOffset is wrong
            // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
            if (!isSuppressed(node.startOffset, fqRuleId, node === rootNode)) {
                try {
                    rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                        // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                        if (node.startOffset != offset && isSuppressed(offset, fqRuleId, node === rootNode)) {
                            return@visit
                        }
                        val (line, col) = positionByOffset(offset)
                        errors.add(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected))
                    }
                } catch (e: Exception) {
                    val (line, col) = positionByOffset(node.startOffset)
                    throw RuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }
        errors
            .sortedWith(Comparator { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col })
            .forEach { e -> params.cb(e, false) }
    }

    private fun userDataResolver(editorConfigPath: String?, debug: Boolean): (String?) -> Map<String, String> {
        if (editorConfigPath != null) {
            val userData = (
                EditorConfigInternal.of(File(editorConfigPath).canonicalPath)
                    ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                    ?: emptyMap<String, String>()
                )
            return fun (fileName: String?) = if (fileName != null) {
                userData + ("file_path" to fileName)
            } else {
                emptyMap()
            }
        }
        val workDir = File(".").canonicalPath
        val workdirUserData = lazy {
            (
                EditorConfigInternal.of(workDir)
                    ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                    ?: emptyMap<String, String>()
                )
        }
        val editorConfig = EditorConfigInternal.cached()
        val editorConfigSet = ConcurrentHashMap<Path, Boolean>()
        return fun (fileName: String?): Map<String, String> {
            if (fileName == null) {
                return emptyMap()
            }

            if (fileName == STDIN_FILE) {
                return workdirUserData.value
            }
            return (
                editorConfig.of(Paths.get(fileName).parent)
                    ?.onlyIf({ debug }) {
                        printEditorConfigChain(it) {
                            editorConfigSet.put(it.path, true) != true
                        }
                    }
                    ?: emptyMap<String, String>()
                ) + ("file_path" to fileName)
        }
    }

    private fun printEditorConfigChain(ec: EditorConfigInternal, predicate: (EditorConfigInternal) -> Boolean = { true }) {
        for (lec in generateSequence(ec) { it.parent }.takeWhile(predicate)) {
            System.err.println(
                "[DEBUG] Discovered .editorconfig (${lec.path.parent.toFile().path})" +
                    " {${lec.entries.joinToString(", ")}}"
            )
        }
    }

    private fun injectUserData(node: ASTNode, userData: Map<String, String>) {
        val android = userData["android"]?.toBoolean() ?: false
        val editorConfigMap =
            if (android &&
                userData["max_line_length"].let { it?.toLowerCase() != "off" && it?.toIntOrNull() == null }
            ) {
                userData + mapOf("max_line_length" to "100")
            } else {
                userData
            }
        node.putUserData(FILE_PATH_USER_DATA_KEY, userData["file_path"])
        node.putUserData(EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(editorConfigMap - "android" - "file_path"))
        node.putUserData(ANDROID_USER_DATA_KEY, android)
        node.putUserData(
            DISABLED_RULES,
            userData["disabled_rules"]?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
        )
    }

    private fun visitor(
        rootNode: ASTNode,
        ruleSets: Iterable<RuleSet>,
        concurrent: Boolean = true,
        filter: (rootNode: ASTNode, fqRuleId: String) -> Boolean = this::filterDisabledRules

    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val fqrsRestrictedToRoot = mutableListOf<Pair<String, Rule>>()
        val fqrs = mutableListOf<Pair<String, Rule>>()
        val fqrsExpectedToBeExecutedLastOnRoot = mutableListOf<Pair<String, Rule>>()
        val fqrsExpectedToBeExecutedLast = mutableListOf<Pair<String, Rule>>()
        for (ruleSet in ruleSets) {
            val prefix = if (ruleSet.id === "standard") "" else "${ruleSet.id}:"
            for (rule in ruleSet) {
                val fqRuleId = "$prefix${rule.id}"
                if (!filter(rootNode, fqRuleId)) {
                    continue
                }
                val fqr = fqRuleId to rule
                when {
                    rule is Rule.Modifier.Last -> fqrsExpectedToBeExecutedLast.add(fqr)
                    rule is Rule.Modifier.RestrictToRootLast -> fqrsExpectedToBeExecutedLastOnRoot.add(fqr)
                    rule is Rule.Modifier.RestrictToRoot -> fqrsRestrictedToRoot.add(fqr)
                    else -> fqrs.add(fqr)
                }
            }
        }
        return { visit ->
            for ((fqRuleId, rule) in fqrsRestrictedToRoot) {
                visit(rootNode, rule, fqRuleId)
            }
            if (concurrent) {
                rootNode.visit { node ->
                    for ((fqRuleId, rule) in fqrs) {
                        visit(node, rule, fqRuleId)
                    }
                }
            } else {
                for ((fqRuleId, rule) in fqrs) {
                    rootNode.visit { node ->
                        visit(node, rule, fqRuleId)
                    }
                }
            }
            for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLastOnRoot) {
                visit(rootNode, rule, fqRuleId)
            }
            if (!fqrsExpectedToBeExecutedLast.isEmpty()) {
                if (concurrent) {
                    rootNode.visit { node ->
                        for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLast) {
                            visit(node, rule, fqRuleId)
                        }
                    }
                } else {
                    for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLast) {
                        rootNode.visit { node ->
                            visit(node, rule, fqRuleId)
                        }
                    }
                }
            }
        }
    }

    private fun filterDisabledRules(rootNode: ASTNode, fqRuleId: String): Boolean {
        return rootNode.getUserData(DISABLED_RULES)?.contains(fqRuleId) == false
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
            if (listOfHints.isEmpty()) nullSuppression else { offset, ruleId, root ->
                if (root) {
                    val h = listOfHints[0]
                    h.range.endInclusive == 0 && (h.disabledRules.isEmpty() || h.disabledRules.contains(ruleId))
                } else {
                    listOfHints.any { (range, disabledRules) ->
                        (disabledRules.isEmpty() || disabledRules.contains(ruleId)) && range.contains(offset)
                    }
                }
            }
        }

    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    fun format(params: Params): String {
        val normalizedText = params.text.replace("\r\n", "\n").replace("\r", "\n")
        val positionByOffset = calculateLineColByOffset(normalizedText)
        val psiFileName = if (params.script) "file.kts" else "file.kt"
        val psiFile = psiFileFactory.createFileFromText(psiFileName, KotlinLanguage.INSTANCE, normalizedText) as KtFile
        val errorElement = psiFile.findErrorElement()
        if (errorElement != null) {
            val (line, col) = positionByOffset(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }
        val rootNode = psiFile.node
        // Passed-in userData overrides .editorconfig
        val mergedUserData = userDataResolver(params.editorConfigPath, params.debug)(params.fileName) + params.userData
        injectUserData(rootNode, mergedUserData)
        var isSuppressed = calculateSuppressedRegions(rootNode)
        var tripped = false
        var mutated = false
        visitor(rootNode, params.ruleSets, concurrent = false)
            .invoke { node, rule, fqRuleId ->
                // fixme: enforcing suppression based on node.startOffset is wrong
                // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                if (!isSuppressed(node.startOffset, fqRuleId, node === rootNode)) {
                    try {
                        rule.visit(node, true) { _, _, canBeAutoCorrected ->
                            tripped = true
                            if (canBeAutoCorrected) {
                                mutated = true
                                if (isSuppressed !== nullSuppression) {
                                    isSuppressed = calculateSuppressedRegions(rootNode)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // line/col cannot be reliably mapped as exception might originate from a node not present
                        // in the original AST
                        throw RuleExecutionException(0, 0, fqRuleId, e)
                    }
                }
            }
        if (tripped) {
            val errors = mutableListOf<Pair<LintError, Boolean>>()
            visitor(rootNode, params.ruleSets).invoke { node, rule, fqRuleId ->
                // fixme: enforcing suppression based on node.startOffset is wrong
                // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                if (!isSuppressed(node.startOffset, fqRuleId, node === rootNode)) {
                    try {
                        rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                            // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                            if (node.startOffset != offset && isSuppressed(offset, fqRuleId, node === rootNode)) {
                                return@visit
                            }
                            val (line, col) = positionByOffset(offset)
                            errors.add(Pair(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected), false))
                        }
                    } catch (e: Exception) {
                        val (line, col) = positionByOffset(node.startOffset)
                        throw RuleExecutionException(line, col, fqRuleId, e)
                    }
                }
            }
            errors
                .sortedWith(Comparator { (l), (r) -> if (l.line != r.line) l.line - r.line else l.col - r.col })
                .forEach { (e, corrected) -> params.cb(e, corrected) }
        }
        return if (mutated) rootNode.text.replace("\n", determineLineSeparator(params.text, params.userData)) else params.text
    }

    private fun determineLineSeparator(fileContent: String, userData: Map<String, String>): String {
        val eol = userData["end_of_line"]?.trim()?.toLowerCase()
        return when {
            eol == "native" -> System.lineSeparator()
            eol == "crlf" || eol != "lf" && fileContent.lastIndexOf('\r') != -1 -> "\r\n"
            else -> "\n"
        }
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
                                                IntRange(openingHint.range.start, node.startOffset),
                                                disabledRules
                                            )
                                        )
                                    }
                                }
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
