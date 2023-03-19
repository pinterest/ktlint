package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import mu.KotlinLogging
import picocli.CommandLine
import java.nio.file.Paths

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@CommandLine.Command(
    description = [
        "Generate kotlin style section for '.editorconfig' file.",
        "Output should be copied manually to the '.editorconfig' file.",
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

        if (ktlintCommand.codeStyle == null) {
            System.err.println("Option --code-style must be set as to generate the '.editorconfig' correctly")
            exitKtLintProcess(1)
        }

        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders = ktlintCommand.ruleProviders(),
                editorConfigOverride = EditorConfigOverride.from(CODE_STYLE_PROPERTY to ktlintCommand.codeStyle),
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

    internal companion object {
        internal const val COMMAND_NAME = "generateEditorConfig"
    }
}
