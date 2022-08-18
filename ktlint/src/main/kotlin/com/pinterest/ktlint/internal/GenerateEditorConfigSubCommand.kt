package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.codeStyleSetProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import picocli.CommandLine

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

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

        // For now we are using CLI invocation dir as path to load existing '.editorconfig'
        val generatedEditorConfig = KtLint.generateKotlinEditorConfigSection(
            KtLint.ExperimentalParams(
                fileName = "./test.kt",
                text = "",
                ruleProviders = ktlintCommand
                    .rulesetJarFiles
                    .loadRuleProviders(
                        ktlintCommand.experimental,
                        ktlintCommand.debug,
                        ktlintCommand.disabledRules,
                    ),
                editorConfigOverride = EditorConfigOverride.from(codeStyleSetProperty to codeStyle()),
                debug = ktlintCommand.debug,
                cb = { _, _ -> },
            ),
        )

        if (generatedEditorConfig.isNotBlank()) {
            // Do not print to logging on purpose. Output below is intended to be copied to ".editofconfig". Users
            // should not be confused with logging markers.
            println("[*.{kt,kts}]\n$generatedEditorConfig")
        } else {
            logger.info { "Nothing to add to .editorconfig file" }
        }
    }

    private fun codeStyle() =
        if (ktlintCommand.android) {
            DefaultEditorConfigProperties.CodeStyleValue.android
        } else {
            DefaultEditorConfigProperties.CodeStyleValue.official
        }

    internal companion object {
        internal const val COMMAND_NAME = "generateEditorConfig"
    }
}
