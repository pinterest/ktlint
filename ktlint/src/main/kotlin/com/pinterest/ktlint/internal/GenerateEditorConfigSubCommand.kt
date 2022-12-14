package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.file.Paths
import mu.KotlinLogging
import picocli.CommandLine

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@CommandLine.Command(
    description = [
        "EXPERIMENTAL!!! Generate kotlin style section for '.editorconfig' file.",
        "Add output content into '.editorconfig' file",
    ],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class GenerateEditorConfigSubCommand : Runnable {
    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        val ktLintRuleEngine = KtLintRuleEngine(
            ruleProviders = ktlintCommand.ruleProviders(),
            editorConfigOverride = EditorConfigOverride.from(CODE_STYLE_PROPERTY to codeStyle()),
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

    private fun codeStyle() =
        if (ktlintCommand.android) {
            CodeStyleValue.android
        } else {
            CodeStyleValue.official
        }

    internal companion object {
        internal const val COMMAND_NAME = "generateEditorConfig"
    }
}
