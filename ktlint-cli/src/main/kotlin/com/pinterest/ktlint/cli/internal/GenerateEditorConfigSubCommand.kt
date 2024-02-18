package com.pinterest.ktlint.cli.internal

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

internal class GenerateEditorConfigSubCommand :
    CliktCommand(
        name = "generateEditorConfig",
        help = "Generate kotlin style section for '.editorconfig' file. Output should be copied manually to the '.editorconfig' file.",
    ) {
    // No default value is set as users should explicitly choose one of the code styles. In this way, it is more clear that the generated
    // content is determined by the chosen value. If a default (ktlint_official) is set, and the user has not specified the code style, the
    // user might not be aware that the value of the other properties are dependent on the code style.
    private val codeStyle by
        option("--code-style", help = "The code style affects the generated settings and their default values.")
            .enum<CodeStyleValue>()
            .required()

    override fun run() {
        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders = (currentContext.parent?.command as KtlintCommandLine).ruleProviders,
                editorConfigOverride = EditorConfigOverride.from(CODE_STYLE_PROPERTY to codeStyle),
                isInvokedFromCli = true,
            )
        val generatedEditorConfig = ktLintRuleEngine.generateKotlinEditorConfigSection(Paths.get("."))

        if (generatedEditorConfig.isNotBlank()) {
            // Do not print to logging on purpose. Output below is intended to be copied to ".editorconfig". Users
            // should not be confused with logging markers.
            println("[*.{kt,kts}]\n$generatedEditorConfig")
        } else {
            LOGGER.info { "Nothing to add to .editorconfig file" }
        }
    }
}
