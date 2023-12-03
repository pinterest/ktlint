package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine
import java.nio.file.Paths

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@CommandLine.Command(
    description = [
        "Generate kotlin style section for '.editorconfig' file. Output should be copied manually to the '.editorconfig' file.",
    ],
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class GenerateEditorConfigSubCommand : Runnable {
    // No default value is set as users should explicitly choose one of the code styles. In this way, it is more clear that the generated
    // content is determined by the chosen value. If a default (ktlint_official) is set, and the user has not specified the code style, the
    // user might not be aware that the value of the other properties are dependent on the code style.
    @CommandLine.Parameters(
        arity = "1",
        paramLabel = "code-style",
        description = [
            "Code style to be used when generating the '.editorconfig'. Value should be one of 'ktlint_official' (recommended), " +
                "'intellij_idea' or 'android_studio'.",
        ],
        converter = [CodeStyleValueConverter::class],
    )
    var codeStyle: CodeStyleValue? = null

    @CommandLine.ParentCommand
    private lateinit var ktlintCommand: KtlintCommandLine

    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        commandSpec.commandLine().printCommandLineHelpOrVersionUsage()

        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders = ktlintCommand.ruleProviders(),
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

    internal companion object {
        internal const val COMMAND_NAME = "generateEditorConfig"
    }
}

private class CodeStyleValueConverter : CommandLine.ITypeConverter<CodeStyleValue> {
    @Throws(Exception::class)
    override fun convert(value: String?): CodeStyleValue =
        when (value?.lowercase()?.replace("-", "_")) {
            null -> CODE_STYLE_PROPERTY.defaultValue
            "ktlint_official" -> CodeStyleValue.ktlint_official
            "android_studio" -> CodeStyleValue.android_studio
            "intellij_idea" -> CodeStyleValue.intellij_idea
            else -> throw IllegalArgumentException("Invalid code style value")
        }
}
