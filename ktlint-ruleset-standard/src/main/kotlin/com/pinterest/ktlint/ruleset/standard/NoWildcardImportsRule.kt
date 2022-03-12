package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective

@OptIn(FeatureInAlphaState::class)
public class NoWildcardImportsRule :
    Rule("no-wildcard-imports"),
    UsesEditorConfigProperties {
    private var allowedWildcardImports: List<PatternEntry> = emptyList()

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        packagesToUseImportOnDemandProperty
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
            allowedWildcardImports = editorConfig.getEditorConfigValue(packagesToUseImportOnDemandProperty)
        }
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
                        hasAlias = false
                    )
                } else {
                    PatternEntry(
                        packageName = import,
                        withSubpackages = false,
                        hasAlias = false
                    )
                }
            }
        }

        private val packagesToUseImportOnDemandPropertyParser: (String, String?) -> PropertyType.PropertyValue<List<PatternEntry>> =
            { _, value ->
                when {
                    else -> try {
                        PropertyType.PropertyValue.valid(
                            value,
                            value?.let(::parseAllowedWildcardImports) ?: emptyList()
                        )
                    } catch (e: IllegalArgumentException) {
                        PropertyType.PropertyValue.invalid(
                            value,
                            "Unexpected imports layout: $value"
                        )
                    }
                }
            }

        public val packagesToUseImportOnDemandProperty: UsesEditorConfigProperties.EditorConfigProperty<List<PatternEntry>> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType(
                    "ij_kotlin_packages_to_use_import_on_demand",
                    "Defines allowed wildcard imports",
                    packagesToUseImportOnDemandPropertyParser
                ),
                /**
                 * Default IntelliJ IDEA style: Use wildcard imports for packages in "java.util", "kotlin.android.synthetic" and
                 * it's subpackages.
                 *
                 * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L81-L82
                 */
                defaultValue = parseAllowedWildcardImports("java.util.*,kotlinx.android.synthetic.**"),
                propertyWriter = { it.joinToString(separator = ",") }
            )
    }
}
