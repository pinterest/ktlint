package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective

public class NoWildcardImportsRule :
    Rule("no-wildcard-imports"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND,
    )

    private lateinit var allowedWildcardImports: List<PatternEntry>

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        allowedWildcardImports = editorConfigProperties.getEditorConfigValue(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND)
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val path = importDirective.importPath ?: return
            if (!path.isAllUnder) return
            if (allowedWildcardImports.none { it.matches(path) }) {
                emit(node.startOffset, "Wildcard import", false)
            }
        }
    }

    public companion object {
        private const val WILDCARD_WITHOUT_SUBPACKAGES = "*"
        private const val WILDCARD_WITH_SUBPACKAGES = "**"

        private fun parseAllowedWildcardImports(allowedWildcardImports: String): List<PatternEntry> {
            val importsList = allowedWildcardImports.split(",").onEach { it.trim() }

            return importsList.map { import ->
                if (import.endsWith(WILDCARD_WITH_SUBPACKAGES)) { // java.**
                    PatternEntry(
                        packageName = import.removeSuffix(WILDCARD_WITH_SUBPACKAGES).plus(WILDCARD_WITHOUT_SUBPACKAGES),
                        withSubpackages = true,
                        hasAlias = false,
                    )
                } else {
                    PatternEntry(
                        packageName = import,
                        withSubpackages = false,
                        hasAlias = false,
                    )
                }
            }
        }

        private val PACKAGES_TO_USE_ON_DEMAND_IMPORT_PROPERTY_PARSER: (String, String?) -> PropertyType.PropertyValue<List<PatternEntry>> =
            { _, value ->
                when {
                    else -> try {
                        PropertyType.PropertyValue.valid(
                            value,
                            value?.let(::parseAllowedWildcardImports) ?: emptyList(),
                        )
                    } catch (e: IllegalArgumentException) {
                        PropertyType.PropertyValue.invalid(
                            value,
                            "Unexpected imports layout: $value",
                        )
                    }
                }
            }

        public val IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND: EditorConfigProperty<List<PatternEntry>> =
            EditorConfigProperty(
                type = PropertyType(
                    "ij_kotlin_packages_to_use_import_on_demand",
                    "Defines allowed wildcard imports",
                    PACKAGES_TO_USE_ON_DEMAND_IMPORT_PROPERTY_PARSER,
                ),
                /**
                 * Default IntelliJ IDEA style: Use wildcard imports for packages in "java.util", "kotlin.android.synthetic" and
                 * it's subpackages.
                 *
                 * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L81-L82
                 */
                /**
                 * Default IntelliJ IDEA style: Use wildcard imports for packages in "java.util", "kotlin.android.synthetic" and
                 * it's subpackages.
                 *
                 * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L81-L82
                 */
                defaultValue = parseAllowedWildcardImports("java.util.*,kotlinx.android.synthetic.**"),
                propertyWriter = { it.joinToString(separator = ",") },
            )

        @Deprecated(
            message = "Marked for removal in KtLint 0.49",
            replaceWith = ReplaceWith("IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND"),
        )
        @Suppress("ktlint:experimental:property-naming")
        public val packagesToUseImportOnDemandProperty: EditorConfigProperty<List<PatternEntry>> =
            IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND
    }
}
