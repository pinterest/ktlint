package com.example.ktlint.api.consumer

import com.example.ktlint.api.consumer.rules.KTLINT_API_CONSUMER_RULE_PROVIDERS
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * This API Consumer shows how the [KtLintRuleEngine] can be invoked and how the amount and format of the logging can be configured.
 * This example uses 'slf4j-simple' as logging provider. But any logging provider that implements SLF4J API can be used depending on your
 * needs.
 */
public fun main() {
    // The KtLint RuleEngine only needs to be instantiated once and can be reused in multiple invocations
    val ktLintRuleEngine =
        KtLintRuleEngine(
            ruleProviders = KTLINT_API_CONSUMER_RULE_PROVIDERS,
            editorConfigOverride =
                EditorConfigOverride.from(
                    INDENT_STYLE_PROPERTY to IndentConfig.IndentStyle.SPACE,
                    INDENT_SIZE_PROPERTY to 4,
                ),
            editorConfigDefaults =
                EditorConfigDefaults.load(
                    path = Paths.get("/some/path/to/editorconfig/file/or/directory"),
                    propertyTypes = KTLINT_API_CONSUMER_RULE_PROVIDERS.propertyTypes(),
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
    ktLintRuleEngine
        .lint(codeFile) { LOGGER.info { "LintViolation reported by KtLint: $it" } }

    LOGGER.info {
        """

        ==============================================
        Run ktlintRuleEngine.format on sample code
        ---
        """.trimIndent()
    }
    ktLintRuleEngine
        .format(codeFile)
        .also { LOGGER.info { "Code formatted by KtLint:\n$it" } }

    LOGGER.info {
        """

        ==============================================
        The amount and format of the logging is configured in file
         'ktlint-api-consumer/src/main/resources/simplelogger.properties'
        """.trimIndent()
    }
}
