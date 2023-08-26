package com.example.ktlint.api.consumer

import com.example.ktlint.api.consumer.rules.KTLINT_API_CONSUMER_RULE_PROVIDERS
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * This API Consumer shows how the [KtLintRuleEngine] can be invoked and how the amount and format of the logging can be configured.
 * This example uses 'slf4j-simple' as logging provider. But any logging provider that implements SLF4J API can be used depending on your
 * needs.
 */
public fun main() {
    // RuleProviders can be supplied by a custom rule provider of the API Consumer itself. This custom rule provider by definition can only
    // provide rules that are available at compile time. But it is also possible to provide rules from jars that are loaded on runtime.
    val ruleProviders = KTLINT_API_CONSUMER_RULE_PROVIDERS.plus(runtimeLoadedRuleProviders)

    // Providing rule dependencies at compile time has the advantage that properties defined in those rules can be used to build the
    // EditorConfigOverride using static types. However, providing the rule dependencies at runtime can offer flexibility to the API
    // Consumer. It comes with the cost that the EditorConfigOverride can not be build with static types.
    val editorConfigOverride =
        EditorConfigOverride
            .from(
                // Property types provided by ktlint-rule-engine-core
                INDENT_STYLE_PROPERTY to IndentConfig.IndentStyle.SPACE,
                INDENT_SIZE_PROPERTY to 4,
                EXPERIMENTAL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled,
                // Properties defines in the ktlint-ruleset-standard can only be used statically when that dependency is provided at compile
                // time.
                // FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY to always
                // For properties that are defined in rules for which the dependency is provided at runtime only, the property name can be
                // provided as String and the value as Any type. In case the property value is invalid, the KtlintRuleEngine logs a warning.
                ruleProviders.findEditorConfigProperty("ktlint_function_signature_body_expression_wrapping") to "alwaysx",
                RuleId("standard:function-signature").createRuleExecutionEditorConfigProperty() to RuleExecution.disabled,
                RuleSetId("some-custom-ruleset").createRuleSetExecutionEditorConfigProperty() to RuleExecution.enabled,
                // In case an unknown property would be provided, an exception is thrown by the helper method
                // ruleProviders.findEditorConfigProperty("unknown_property") to "some-value",
            )

    // The KtLint RuleEngine only needs to be instantiated once and can be reused in multiple invocations
    val apiConsumerKtLintRuleEngine =
        KtLintRuleEngine(
            ruleProviders = ruleProviders,
            editorConfigOverride = editorConfigOverride,
            editorConfigDefaults =
                EditorConfigDefaults.load(
                    path = Paths.get("/some/path/to/editorconfig/file/or/directory"),
                    propertyTypes = ruleProviders.propertyTypes(),
                ),
        )

    val codeFile =
        "ktlint-api-consumer/src/main/kotlin/com/example/ktlint/api/consumer/KtlintApiConsumer.kt"
            .let { fileName ->
                LOGGER.info { "Read code from file '$fileName'" }
                Code.fromFile(
                    File(fileName),
                )
            }

    LOGGER.info {
        """

        ==============================================
        Run ktlintRuleEngine.lint on sample code
        ---
        """.trimIndent()
    }
    apiConsumerKtLintRuleEngine
        .lint(codeFile) {
            LOGGER.info { "LintViolation reported by KtLint: $it" }
        }

    LOGGER.info {
        """

        ==============================================
        Run ktlintRuleEngine.format on sample code
        ---
        """.trimIndent()
    }
    apiConsumerKtLintRuleEngine
        .format(codeFile)
        .also {
            LOGGER.info { "Code formatted by KtLint:\n$it" }
        }

    LOGGER.info {
        """

        ==============================================
        The amount and format of the logging is configured in file
         'ktlint-api-consumer/src/main/resources/simplelogger.properties'
        """.trimIndent()
    }
}

private val runtimeLoadedRuleProviders =
    try {
        ServiceLoader
            .load(
                RuleSetProviderV3::class.java,
                URLClassLoader(emptyArray<URL?>()),
            ).flatMap { it.getRuleProviders() }
            .toSet()
    } catch (e: ServiceConfigurationError) {
        LOGGER.warn { "Error while loading the rulesets:\n${e.printStackTrace()}" }
        emptySet()
    }

/**
 * Finds the first [EditorConfigProperty] with name [propertyName] used by any of the [RuleProvider]s.
 */
private fun Set<RuleProvider>.findEditorConfigProperty(propertyName: String): EditorConfigProperty<*> {
    val properties = editorConfigProperties()
    return properties.findProperty(propertyName)
        ?: throw EditorConfigPropertyNotFoundException(
            properties
                .map { it.type.name }
                .sorted()
                .joinToString(
                    prefix = "Property with name '$propertyName' is not found in any of given rules. Available properties:\n\t",
                    separator = "\n\t",
                    postfix = "Next to properties above, the properties to enable or disable rules are allowed as well."
                ) { "- $it" },
        )
}

private fun Set<RuleProvider>.editorConfigProperties() =
    map { it.createNewRuleInstance() }
        .flatMap { it.usesEditorConfigProperties }
        .distinct()

private fun List<EditorConfigProperty<*>>.findProperty(propertyName: String): EditorConfigProperty<*>? =
    find { it.type.name == propertyName }

public class EditorConfigPropertyNotFoundException(
    message: String,
) : RuntimeException(message)
