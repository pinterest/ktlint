package com.example.ktlint.api.consumer

import com.example.ktlint.api.consumer.rules.KTLINT_API_CONSUMER_RULE_PROVIDERS
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import mu.KotlinLogging
import java.io.File

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

public class KtlintApiConsumer {
    // The KtLint RuleEngine only needs to be instantiated once and can be reused in multiple invocations
    private val ktLintRuleEngine = KtLintRuleEngine(
        ruleProviders = KTLINT_API_CONSUMER_RULE_PROVIDERS,
    )

    public fun run(
        command: String,
        fileName: String,
    ) {
        val codeFile = Code.CodeFile(
            File(fileName),
        )

        when (command) {
            "lint" -> {
                ktLintRuleEngine
                    .lint(codeFile) {
                        LOGGER.info { "LintViolation reported by KtLint: $it" }
                    }
            }
            "format" -> {
                ktLintRuleEngine
                    .format(codeFile)
                    .also {
                        LOGGER.info { "Code formatted by KtLint:\n$it" }
                    }
            }
            else -> LOGGER.error { "Unexpected argument '$command'" }
        }
    }
}
