package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.EditorConfig
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule.Companion.ASCII_PATTERN
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule.Companion.IDEA_PATTERN
import com.pinterest.ktlint.ruleset.standard.internal.importordering.ImportSorter
import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Import ordering is configured via EditorConfig's custom property `kotlin_imports_layout`. Supported values:
 * * "idea" - default IntelliJ IDEA's order, see [IDEA_PATTERN]
 * * "ascii" - alphabetical order as recommended in Android's Kotlin style guide, see [ASCII_PATTERN]
 * * custom - defined by the following set of tokens. Tokens can be combined together in a group, groups/tokens must be comma separated:
 *  * "*" - wildcard symbol, can be used as follows:
 *      1. Single, meaning matching any import (<all other imports> in IDEA)
 *      2. After an import path, e.g. "java.*" or "kotlin.io.*"
 *      3. In conjunction with "^" operator, meaning matching any alias import - "^*" (<all other alias imports> in IDEA)
 *  * "|" - blank line symbol. Only supported single blank lines between imports. Multiple blank lines will be ignored. Blank lines are not allowed outside of import list.
 *  * "^" - alias symbol, can be used as follows:
 *      1. In front of an import path, meaning matching all alias imports from this path, e.g. "^android.*"
 *      2. In conjunction with "*" operator, meaning matching any alias import - "^*" (<all other alias imports> in IDEA)
 *  * import paths - these can be full paths, e.g. "java.util.List" as well as wildcard paths, e.g. "kotlin.*"
 *
 * In case the custom property is not provided, the rule defaults to "ascii" style in case of "android" flag supplied, or to "idea" otherwise.
 */
class ImportOrderingRule : Rule("import-ordering") {

    private lateinit var importsLayout: String
    private lateinit var importSorter: ImportSorter

    companion object {
        /**
         * Alphabetical with capital letters before lower case letters (e.g. Z before a).
         * No blank lines between major groups (android, com, junit, net, org, java, javax).
         * Single group regardless of import type.
         *
         * https://developer.android.com/kotlin/style-guide#import_statements
         */
        private const val ASCII_PATTERN = "*"

        /**
         * Default IntelliJ IDEA style: Alphabetical with capital letters before lower case letters (e.g. Z before a),
         * except such groups as "java", "javax" and "kotlin" that are placed in the end. Within the groups the alphabetical order is preserved.
         * Alias imports are placed in a separate group in the end of the list with alphabetical order inside.
         * No blank lines between groups.
         *
         * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L87-L91
         */
        private const val IDEA_PATTERN = "*,java.*,javax.*,kotlin.*,^*"

        private const val IDEA_ERROR_MESSAGE = "Imports must be ordered in lexicographic order without any empty lines in-between " +
            "with \"java\", \"javax\", \"kotlin\" and aliases in the end"
        private const val ASCII_ERROR_MESSAGE = "Imports must be ordered in lexicographic order without any empty lines in-between"
        private const val CUSTOM_ERROR_MESSAGE = "Imports must be ordered according to the pattern specified in .editorconfig"

        private val errorMessages = mapOf(
            IDEA_PATTERN to IDEA_ERROR_MESSAGE,
            ASCII_PATTERN to ASCII_ERROR_MESSAGE
        )
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY) ?: false
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            importsLayout = editorConfig.resolveImportsLayout(android)
            importSorter = ImportSorter(importsLayout)
            return
        }

        if (node.elementType == ElementType.IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                val imports = children.filter {
                    it.elementType == ElementType.IMPORT_DIRECTIVE ||
                        it.psi is PsiWhiteSpace && it.textLength > 1 // also collect empty lines, that are represented as "\n\n"
                }
                val hasComments = children.find { it.elementType == ElementType.BLOCK_COMMENT || it.elementType == ElementType.EOL_COMMENT } != null
                val sortedImports = imports
                    .asSequence()
                    .filter { it.psi !is PsiWhiteSpace } // sorter expects KtImportDirective, whitespaces are inserted afterwards
                    .map { it.psi as KtImportDirective }
                    .sortedWith(importSorter)
                    .distinctBy { if (it.aliasName != null) it.text.substringBeforeLast(it.aliasName!!) else it.text } // distinguish by import path w/o aliases
                    .map { it.node } // transform back to ASTNode in order to operate over its method (addChild)

                // insert blank lines wherever needed
                // traverse the list using fold to have previous and current element and decide if the blank line is needed in between
                // based on the ImportSorter imported patterns and indexes from the comparator
                val sortedImportsWithSpaces = mutableListOf<ASTNode>()
                sortedImports.fold(null as ASTNode?) { prev, current ->
                    val index1 = if (prev == null) -1 else importSorter.findImportIndex((prev.psi as KtImportDirective).importPath!!)
                    val index2 = importSorter.findImportIndex((current.psi as KtImportDirective).importPath!!)

                    var hasBlankLines = false
                    for (i in (index1 + 1) until index2) {
                        if (importSorter.patterns[i] == PatternEntry.BLANK_LINE_ENTRY) {
                            hasBlankLines = true
                            break
                        }
                    }
                    if (hasBlankLines) {
                        sortedImportsWithSpaces += PsiWhiteSpaceImpl("\n\n")
                    }
                    sortedImportsWithSpaces += current

                    return@fold current
                }

                val canAutoCorrect = !hasComments
                if (!importsAreEqual(imports, sortedImportsWithSpaces) || (hasTooMuchWhitespace(children) && !isCustomLayout())) {
                    val additionalMessage = if (!canAutoCorrect) {
                        " -- no autocorrection due to comments in the import list"
                    } else {
                        ""
                    }
                    emit(
                        node.startOffset,
                        "${errorMessages.getOrDefault(importsLayout, CUSTOM_ERROR_MESSAGE)}$additionalMessage",
                        canAutoCorrect
                    )
                    if (autoCorrect && canAutoCorrect) {
                        node.removeRange(node.firstChildNode, node.lastChildNode.treeNext)
                        sortedImportsWithSpaces.reduce { current, next ->
                            node.addChild(current, null)
                            if (current !is PsiWhiteSpace && next !is PsiWhiteSpace) {
                                node.addChild(PsiWhiteSpaceImpl("\n"), null)
                            }
                            return@reduce next
                        }
                        node.addChild(sortedImportsWithSpaces.last(), null)
                    }
                }
            }
        }
    }

    private fun EditorConfig.resolveImportsLayout(android: Boolean): String {
        val defaultPattern = if (android) "ascii" else "idea"
        val layout = when {
            !get("kotlin_imports_layout").isNullOrEmpty() -> get("kotlin_imports_layout")!!
            !get("ij_kotlin_imports_layout").isNullOrEmpty() -> get("ij_kotlin_imports_layout")!!
            else -> defaultPattern // default to ascii (android) or idea in case there's no such entry in .editorconfig
        }

        return when (layout) { // transform predefined styles into patterns
            "idea" -> IDEA_PATTERN
            "ascii" -> ASCII_PATTERN
            else -> layout
        }
    }

    private fun importsAreEqual(actual: List<ASTNode>, expected: List<ASTNode>): Boolean {
        if (actual.size != expected.size) return false

        val combined = actual.zip(expected)
        return combined.all { (first, second) ->
            if (first is PsiWhiteSpace && second is PsiWhiteSpace) {
                return@all (first as PsiWhiteSpace).text == (second as PsiWhiteSpace).text
            }
            return@all first == second
        }
    }

    private fun isCustomLayout() = importsLayout != IDEA_PATTERN && importsLayout != ASCII_PATTERN

    private fun hasTooMuchWhitespace(nodes: Array<ASTNode>): Boolean {
        return nodes.any { it is PsiWhiteSpace && (it as PsiWhiteSpace).text != "\n" }
    }
}
