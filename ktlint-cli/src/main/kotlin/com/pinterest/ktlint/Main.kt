@file:JvmName("Main")

package com.pinterest.ktlint

import com.github.ajalt.clikt.core.subcommands
import com.pinterest.ktlint.cli.internal.GenerateEditorConfigSubCommand
import com.pinterest.ktlint.cli.internal.GitPreCommitHookSubCommand
import com.pinterest.ktlint.cli.internal.GitPrePushHookSubCommand
import com.pinterest.ktlint.cli.internal.KtlintCommandLine

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
