@file:JvmName("Main")

package io.github.ktlint.core

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.github.ktlint.core.cli.internal.GenerateEditorConfigSubCommand
import io.github.ktlint.core.cli.internal.GitPreCommitHookSubCommand
import io.github.ktlint.core.cli.internal.GitPrePushHookSubCommand
import io.github.ktlint.core.cli.internal.KtlintCommandLine

// Ideally this file would have been moved to the cli package as well. This however is breaking change that is likely to affect each project
// that use either the Maven or Gradle and calls the Ktlint CLI. As those users likely will not read the changelog, this could lead to many
// issues. So the class is to be kept at the old location.
public fun main(args: Array<String>) {
    KtlintCommandLine()
        .subcommands(
            GenerateEditorConfigSubCommand(),
            GitPreCommitHookSubCommand(),
            GitPrePushHookSubCommand(),
        ).main(args)
}
