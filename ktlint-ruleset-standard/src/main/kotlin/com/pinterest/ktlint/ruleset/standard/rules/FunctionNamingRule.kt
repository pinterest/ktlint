package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CommaSeparatedListValueParser
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * https://kotlinlang.org/docs/coding-conventions.html#function-names
 */
@SinceKtlint("0.48", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class FunctionNamingRule :
    StandardRule(
        id = "function-naming",
        usesEditorConfigProperties = setOf(IGNORE_WHEN_ANNOTATED_WITH_PROPERTY),
    ) {
    private var isTestClass = false
    private var ignoreWhenAnnotatedWith = IGNORE_WHEN_ANNOTATED_WITH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        ignoreWhenAnnotatedWith = editorConfig[IGNORE_WHEN_ANNOTATED_WITH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!isTestClass && node.elementType == IMPORT_DIRECTIVE) {
            (node.psi as KtImportDirective)
                .importPath
                ?.pathStr
                ?.takeIf { importPathString -> TEST_LIBRARIES_SET.any { importPathString.startsWith(it) } }
                ?.let { isTestClass = true }
        }

        node
            .takeIf { node.elementType == FUN }
            ?.takeUnless {
                node.isFactoryMethod() ||
                    node.isMethodInTestClass() ||
                    node.hasValidFunctionName() ||
                    node.isAnonymousFunction() ||
                    node.isOverrideFunction() ||
                    node.isAnnotatedWithAnyOf(ignoreWhenAnnotatedWith)
            }?.let {
                val identifierOffset =
                    node
                        .findChildByType(IDENTIFIER)
                        ?.startOffset
                        ?: 1
                emit(
                    identifierOffset,
                    "Function name should start with a lowercase letter (except factory methods) and use camel case",
                    false,
                )
            }
    }

    private fun ASTNode.isFactoryMethod() =
        (this.psi as KtFunction)
            .let { it.hasDeclaredReturnType() && it.name == it.typeReference?.text }

    private fun ASTNode.isMethodInTestClass() = isTestClass && hasValidTestFunctionName()

    private fun ASTNode.hasValidTestFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_TEST_FUNCTION_NAME_REGEXP)

    private fun ASTNode.hasValidFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_FUNCTION_NAME_REGEXP)

    private fun ASTNode.isAnonymousFunction() =
        VALUE_PARAMETER_LIST ==
            findChildByType(FUN_KEYWORD)
                ?.nextCodeSibling()
                ?.elementType

    /*
     * A function override should not be reported as the interface of class that defines the function might be out of scope of the project
     * in which case the function name can not be changed. Note that the function will still be reported at the interface or class itself
     * whenever that interface or class is defined inside the scope of the project.
     */
    private fun ASTNode.isOverrideFunction() =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == OVERRIDE_KEYWORD }

    private fun ASTNode.isAnnotatedWithAnyOf(excludeWhenAnnotatedWith: Set<String>) =
        findChildByType(MODIFIER_LIST).containsAnnotationEntryWithIdentifierIn(excludeWhenAnnotatedWith)

    private fun ASTNode?.containsAnnotationEntryWithIdentifierIn(excludeWhenAnnotatedWith: Set<String>): Boolean =
        this
            ?.children()
            ?.any {
                when (it.elementType) {
                    ANNOTATION -> {
                        it.containsAnnotationEntryWithIdentifierIn(excludeWhenAnnotatedWith)
                    }
                    ANNOTATION_ENTRY -> {
                        it.annotationEntryName() in excludeWhenAnnotatedWith
                    }
                    else -> false
                }
            }
            ?: false

    private fun ASTNode.annotationEntryName() =
        findChildByType(ElementType.CONSTRUCTOR_CALLEE)
            ?.findChildByType(ElementType.TYPE_REFERENCE)
            ?.findChildByType(ElementType.USER_TYPE)
            ?.findChildByType(ElementType.REFERENCE_EXPRESSION)
            ?.findChildByType(IDENTIFIER)
            ?.text

    public companion object {
        public val IGNORE_WHEN_ANNOTATED_WITH_PROPERTY: EditorConfigProperty<Set<String>> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_function_naming_ignore_when_annotated_with",
                        "Ignore functions that are annotated with. Value is a comma separated list of name without the '@' prefix.",
                        CommaSeparatedListValueParser(),
                    ),
                defaultValue = setOf("unset"),
            )

        private val VALID_FUNCTION_NAME_REGEXP = "[a-z][A-Za-z\\d]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        private val VALID_TEST_FUNCTION_NAME_REGEXP = "(`.*`)|([a-z][A-Za-z\\d_]*)".regExIgnoringDiacriticsAndStrokesOnLetters()
        private val TEST_LIBRARIES_SET =
            setOf(
                "io.kotest",
                "kotlin.test",
                "org.junit",
                "org.testng",
            )
    }
}

public val FUNCTION_NAMING_RULE_ID: RuleId = FunctionNamingRule().ruleId
