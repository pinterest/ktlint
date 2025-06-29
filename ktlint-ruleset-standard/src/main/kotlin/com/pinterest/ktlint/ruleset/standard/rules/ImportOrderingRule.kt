package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.ImportOrderingRule.Companion.ASCII_PATTERN
import com.pinterest.ktlint.ruleset.standard.rules.ImportOrderingRule.Companion.IDEA_PATTERN
import com.pinterest.ktlint.ruleset.standard.rules.internal.importordering.ImportSorter
import com.pinterest.ktlint.ruleset.standard.rules.internal.importordering.PatternEntry
import com.pinterest.ktlint.ruleset.standard.rules.internal.importordering.parseImportsLayout
import io.github.oshai.kotlinlogging.KotlinLogging
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtImportDirective

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Import ordering is configured via EditorConfig's property `ij_kotlin_imports_layout`, so the Kotlin IDE plugin also recognizes it. Supported values:
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
@SinceKtlint("0.10", STABLE)
public class ImportOrderingRule :
    StandardRule(
        id = "import-ordering",
        usesEditorConfigProperties = setOf(IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY),
    ) {
    private lateinit var importsLayout: List<PatternEntry>
    private lateinit var importSorter: ImportSorter

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        importsLayout = editorConfig[IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY]
        importSorter = ImportSorter(importsLayout)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                // Get unique imports and blank lines
                val (autoCorrectDuplicateImports: Boolean, imports: List<ASTNode>) =
                    getUniqueImportsAndBlankLines(children, emit)

                val hasComments = children.any { it.elementType == BLOCK_COMMENT || it.elementType == EOL_COMMENT }
                val sortedImports =
                    imports
                        .asSequence()
                        .mapNotNull {
                            // sorter expects KtImportDirective, whitespaces are inserted afterwards
                            it.psi as? KtImportDirective
                        }.sortedWith(importSorter)
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
                    if (hasBlankLines && prev != null) {
                        sortedImportsWithSpaces += PsiWhiteSpaceImpl("\n\n")
                    }
                    sortedImportsWithSpaces += current

                    return@fold current
                }

                if (hasComments) {
                    emit(
                        node.startOffset,
                        ERROR_MESSAGES.getOrDefault(importsLayout, CUSTOM_ERROR_MESSAGE) +
                            " -- no autocorrection due to comments in the import list",
                        false,
                    )
                } else {
                    val autoCorrectWhitespace = hasTooMuchWhitespace(children) && !isCustomLayout()
                    val autoCorrectSortOrder = !importsAreEqual(imports, sortedImportsWithSpaces)
                    var autocorrect = autoCorrectDuplicateImports
                    if (autoCorrectSortOrder || autoCorrectWhitespace) {
                        emit(
                            node.startOffset,
                            ERROR_MESSAGES.getOrDefault(importsLayout, CUSTOM_ERROR_MESSAGE),
                            true,
                        ).ifAutocorrectAllowed { autocorrect = true }
                    }
                    if (autocorrect) {
                        node.removeRange(node.firstChildNode, node.lastChildNode.nextSibling20)
                        sortedImportsWithSpaces.reduce { current, next ->
                            node.addChild(current, null)
                            if (!current.isWhiteSpace20 && !next.isWhiteSpace20) {
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ): Pair<Boolean, List<ASTNode>> {
        var autoCorrectDuplicateImports = false
        val imports = mutableListOf<ASTNode>()
        val importTextSet = mutableSetOf<String>()

        children.forEach { current ->
            when {
                current.isWhiteSpace20 && current.text.count { it == '\n' } > 1 -> {
                    imports += current
                }

                current.elementType == IMPORT_DIRECTIVE -> {
                    if (importTextSet.add(current.text)) {
                        imports += current
                    } else {
                        emit(current.startOffset, "Duplicate '${current.text}' found", true)
                            .ifAutocorrectAllowed { autoCorrectDuplicateImports = true }
                    }
                }
            }
        }

        return autoCorrectDuplicateImports to imports
    }

    private fun importsAreEqual(
        actual: List<ASTNode>,
        expected: List<ASTNode>,
    ): Boolean {
        if (actual.size != expected.size) return false

        val combined = actual.zip(expected)
        return combined.all { (first, second) ->
            if (first.isWhiteSpace20 && second.isWhiteSpace20) {
                return@all (first as PsiWhiteSpace).text == (second as PsiWhiteSpace).text
            }
            return@all first == second
        }
    }

    private fun isCustomLayout() = importsLayout != IDEA_PATTERN && importsLayout != ASCII_PATTERN

    private fun hasTooMuchWhitespace(nodes: Array<ASTNode>): Boolean = nodes.any { it.isWhiteSpaceWithoutNewline20 }

    public companion object {
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

        private const val IDEA_ERROR_MESSAGE =
            "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and " +
                "aliases in the end"
        private const val ASCII_ERROR_MESSAGE = "Imports must be ordered in lexicographic order without any empty lines in-between"
        private const val CUSTOM_ERROR_MESSAGE = "Imports must be ordered according to the pattern specified in .editorconfig"

        private val ERROR_MESSAGES =
            mapOf(
                IDEA_PATTERN to IDEA_ERROR_MESSAGE,
                ASCII_PATTERN to ASCII_ERROR_MESSAGE,
            )

        private val EDITOR_CONFIG_PROPERTY_PARSER: (String, String?) -> PropertyType.PropertyValue<List<PatternEntry>> =
            { _, value ->
                when {
                    value.isNullOrBlank() -> {
                        PropertyType.PropertyValue.invalid(
                            value,
                            "Import layout must contain at least one entry of a wildcard symbol (*)",
                        )
                    }

                    value == "idea" -> {
                        LOGGER.warn {
                            "`idea` is deprecated! Please use `*,java.**,javax.**,kotlin.**,^` instead to ensure that the Kotlin IDE " +
                                "plugin recognizes the value"
                        }
                        PropertyType.PropertyValue.valid(
                            value,
                            IDEA_PATTERN,
                        )
                    }

                    value == "ascii" -> {
                        LOGGER.warn {
                            "`ascii` is deprecated! Please use `*` instead to ensure that the Kotlin IDE plugin recognizes the value"
                        }
                        PropertyType.PropertyValue.valid(
                            value,
                            ASCII_PATTERN,
                        )
                    }

                    else -> {
                        try {
                            PropertyType.PropertyValue.valid(
                                value,
                                parseImportsLayout(value),
                            )
                        } catch (e: IllegalArgumentException) {
                            PropertyType.PropertyValue.invalid(
                                value,
                                "Unexpected imports layout: $value",
                            )
                        }
                    }
                }
            }

        public val IJ_KOTLIN_IMPORTS_LAYOUT_PROPERTY: EditorConfigProperty<List<PatternEntry>> =
            EditorConfigProperty<List<PatternEntry>>(
                type =
                    PropertyType(
                        "ij_kotlin_imports_layout",
                        "Defines imports order layout for Kotlin files",
                        EDITOR_CONFIG_PROPERTY_PARSER,
                    ),
                defaultValue = IDEA_PATTERN,
                androidStudioCodeStyleDefaultValue = ASCII_PATTERN,
                propertyWriter = { it.joinToString(separator = ",") },
            )
    }
}

public val IMPORT_ORDERING_RULE_ID: RuleId = ImportOrderingRule().ruleId
