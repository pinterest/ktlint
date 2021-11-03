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
public class NoWildcardImportsRule : Rule("no-wildcard-imports"), UsesEditorConfigProperties {
    private var allowedWildcardImports: List<PatternEntry> = emptyList()

    public companion object {
        internal const val IDEA_PACKAGES_TO_USE_IMPORT_ON_DEMAND_PROPERTY_NAME = "ij_kotlin_packages_to_use_import_on_demand"
        private const val PROPERTY_DESCRIPTION = "Defines allowed wildcard imports"

        /**
         * Default IntelliJ IDEA style: Use wildcard imports for packages in "java.util", "kotlin.android.synthetic" and
         * it's subpackages.
         *
         * https://github.com/JetBrains/kotlin/blob/ffdab473e28d0d872136b910eb2e0f4beea2e19c/idea/formatter/src/org/jetbrains/kotlin/idea/core/formatter/KotlinCodeStyleSettings.java#L81-L82
         */
        private val IDEA_PATTERN = parseAllowedWildcardImports("java.util.*,kotlinx.android.synthetic.**")

        private val editorConfigPropertyParser: (String, String?) -> PropertyType.PropertyValue<List<PatternEntry>> =
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

        public val ideaPackagesToUseImportOnDemandProperty: UsesEditorConfigProperties.EditorConfigProperty<List<PatternEntry>> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType(
                    IDEA_PACKAGES_TO_USE_IMPORT_ON_DEMAND_PROPERTY_NAME,
                    PROPERTY_DESCRIPTION,
                    editorConfigPropertyParser
                ),
                defaultValue = IDEA_PATTERN,
                propertyWriter = { it.joinToString(separator = ",") }
            )
    }

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        ideaPackagesToUseImportOnDemandProperty
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
            allowedWildcardImports = editorConfig.getEditorConfigValue(ideaPackagesToUseImportOnDemandProperty)
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
}

internal const val WILDCARD_CHAR = "*"

internal fun parseAllowedWildcardImports(allowedWildcardImports: String): List<PatternEntry> {
    val importsList = allowedWildcardImports.split(",").onEach { it.trim() }

    return importsList.map {
        var import = it
        var withSubpackages = false
        if (import.endsWith(WILDCARD_CHAR + WILDCARD_CHAR)) { // java.**
            import = import.substringBeforeLast(WILDCARD_CHAR)
            withSubpackages = true
        }

        PatternEntry(import, withSubpackages, false)
    }
}
