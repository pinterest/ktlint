package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule.Companion.ASCII_PATTERN
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule.Companion.IDEA_PATTERN
import com.pinterest.ktlint.ruleset.standard.internal.importordering.ImportSorter
import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import com.pinterest.ktlint.ruleset.standard.internal.importordering.parseImportsLayout
import mu.KotlinLogging
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtImportDirective

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Import ordering is configured via EditorConfig's property `ij_kotlin_imports_layout`, so the Kotlin IDE plugin also recongizes it. Supported values:
 * * "*,java.**,javax.**,kotlin.**,^" - default IntelliJ IDEA's order, see [IDEA_PATTERN]
 * * "*" - alphabetical order as recommended in Android's Kotlin style guide, see [ASCII_PATTERN]
 * * custom - defined by the following set of tokens. Tokens can be combined together in a group, groups/tokens must be comma separated:
 *  * "*" - wildcard symbol, can be used as follows:
 *      1. Single, meaning matching any import (<all other imports> in IDEA)
 *      2. After an import path, e.g. "java.*" or "kotlin.io.Closeable.*"
 *      3. Doubled after an import path, e.g. "java.**" or "kotlin.io.**", meaning "with subpackages"
 *  * "|" - blank line symbol. Only supported single blank lines between imports. Multiple blank lines will be ignored. Blank lines are not allowed outside of import list.
 *  * "^" - alias symbol, can be used as follows:
 *      1. In front of an import path, meaning matching all alias imports from this path, e.g. "^android.*"
 *      2. Alone, meaning matching any alias import - "^" (<all other alias imports> in IDEA)
 *  * import paths - these can be full paths, e.g. "java.util.List.*" as well as wildcard paths meaning "with subpackages", e.g. "kotlin.**"
 *
 * In case the custom property is not provided, the rule defaults to alphabetical order in case of "android" flag supplied, or to idea otherwise.
 */
public class ImportOrderingRule :
    Rule("import-ordering"),
    UsesEditorConfigProperties {

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        ideaImportsLayoutProperty
    )

    private lateinit var importsLayout: List<PatternEntry>
    private lateinit var importSorter: ImportSorter

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        importsLayout = editorConfigProperties.getEditorConfigValue(ideaImportsLayoutProperty)
        importSorter = ImportSorter(importsLayout)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == ElementType.IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                // Get unique imports and blank lines
                val (autoCorrectDuplicateImports: Boolean, imports: List<ASTNode>) =
                    getUniqueImportsAndBlankLines(children, emit)

                val hasComments = children.any { it.elementType == ElementType.BLOCK_COMMENT || it.elementType == ElementType.EOL_COMMENT }
                val sortedImports = imports
                    .asSequence()
                    .mapNotNull { it.psi as? KtImportDirective } // sorter expects KtImportDirective, whitespaces are inserted afterwards
                    .sortedWith(importSorter)
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

                if (hasComments) {
                    emit(
                        node.startOffset,
                        errorMessages.getOrDefault(importsLayout, CUSTOM_ERROR_MESSAGE) +
                            " -- no autocorrection due to comments in the import list",
                        false
                    )
                } else {
                    val autoCorrectWhitespace = hasTooMuchWhitespace(children) && !isCustomLayout()
                    val autoCorrectSortOrder = !importsAreEqual(imports, sortedImportsWithSpaces)
                    if (autoCorrectSortOrder || autoCorrectWhitespace) {
                        emit(
                            node.startOffset,
                            errorMessages.getOrDefault(importsLayout, CUSTOM_ERROR_MESSAGE),
                            true
                        )
                    }
                    if (autoCorrect && (autoCorrectDuplicateImports || autoCorrectSortOrder || autoCorrectWhitespace)) {
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

    private fun getUniqueImportsAndBlankLines(
        children: Array<ASTNode>,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ): Pair<Boolean, List<ASTNode>> {
        var autoCorrectDuplicateImports = false
        val imports = mutableListOf<ASTNode>()
        val importTextSet = mutableSetOf<String>()

        children.forEach { current ->
            val isPsiWhiteSpace = current.psi is PsiWhiteSpace

            if (current.elementType == ElementType.IMPORT_DIRECTIVE ||
                isPsiWhiteSpace && current.textLength > 1 // also collect empty lines, that are represented as "\n\n"
            ) {
                if (isPsiWhiteSpace || importTextSet.add(current.text)) {
                    imports += current
                } else {
                    emit(
                        current.startOffset,
                        "Duplicate '${current.text}' found",
                        true
                    )
                    autoCorrectDuplicateImports = true
                }
            }
        }

        return autoCorrectDuplicateImports to imports
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

    public companion object {
        internal const val IDEA_IMPORTS_LAYOUT_PROPERTY_NAME = "ij_kotlin_imports_layout"
        private const val PROPERTY_DESCRIPTION = "Defines imports order layout for Kotlin files"

        /**
         * Alphabetical with capital letters before lower case letters (e.g. Z before a).
         * No blank lines between major groups (android, com, junit, net, org, java, javax).
         * Single group regardless of import type.
         *
         * https://developer.android.com/kotlin/style-guide#import_statements
         */
        private val ASCII_PATTERN = parseImportsLayout("*")

        /**
         * Default IntelliJ IDEA style: Alphabetical with capital letters before lower case letters (e.g. Z before a),
         * except such groups as "java", "javax" and "kotlin" that are placed in the end. Within the groups the alphabetical order is preserved.
         * Alias imports are placed in a separate group in the end of the list with alphabetical order inside.
         * No blank lines between groups.
         *
         * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L87-L91
         */
        private val IDEA_PATTERN = parseImportsLayout("*,java.**,javax.**,kotlin.**,^")

        private const val IDEA_ERROR_MESSAGE = "Imports must be ordered in lexicographic order without any empty lines in-between " +
            "with \"java\", \"javax\", \"kotlin\" and aliases in the end"
        private const val ASCII_ERROR_MESSAGE = "Imports must be ordered in lexicographic order without any empty lines in-between"
        private const val CUSTOM_ERROR_MESSAGE = "Imports must be ordered according to the pattern specified in .editorconfig"

        private val errorMessages = mapOf(
            IDEA_PATTERN to IDEA_ERROR_MESSAGE,
            ASCII_PATTERN to ASCII_ERROR_MESSAGE
        )

        private val editorConfigPropertyParser: (String, String?) -> PropertyType.PropertyValue<List<PatternEntry>> =
            { _, value ->
                when {
                    value.isNullOrBlank() -> PropertyType.PropertyValue.invalid(
                        value,
                        "Import layout must contain at least one entry of a wildcard symbol (*)"
                    )
                    value == "idea" -> {
                        logger.warn { "`idea` is deprecated! Please use `*,java.**,javax.**,kotlin.**,^` instead to ensure that the Kotlin IDE plugin recognizes the value" }
                        PropertyType.PropertyValue.valid(
                            value,
                            IDEA_PATTERN
                        )
                    }
                    value == "ascii" -> {
                        logger.warn { "`ascii` is deprecated! Please use `*` instead to ensure that the Kotlin IDE plugin recognizes the value" }
                        PropertyType.PropertyValue.valid(
                            value,
                            ASCII_PATTERN
                        )
                    }
                    else -> try {
                        PropertyType.PropertyValue.valid(
                            value,
                            parseImportsLayout(value)
                        )
                    } catch (e: IllegalArgumentException) {
                        PropertyType.PropertyValue.invalid(
                            value,
                            "Unexpected imports layout: $value"
                        )
                    }
                }
            }

        public val ideaImportsLayoutProperty: UsesEditorConfigProperties.EditorConfigProperty<List<PatternEntry>> =
            UsesEditorConfigProperties.EditorConfigProperty<List<PatternEntry>>(
                type = PropertyType(
                    IDEA_IMPORTS_LAYOUT_PROPERTY_NAME,
                    PROPERTY_DESCRIPTION,
                    editorConfigPropertyParser
                ),
                defaultValue = IDEA_PATTERN,
                defaultAndroidValue = ASCII_PATTERN,
                propertyWriter = { it.joinToString(separator = ",") }
            )
    }
}
