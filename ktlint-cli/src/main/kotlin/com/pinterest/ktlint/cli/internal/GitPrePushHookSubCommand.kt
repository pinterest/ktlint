package com.pinterest.ktlint.cli.internal

internal class GitPrePushHookSubCommand :
    GitHookCliktCommand(
        name = "installGitPrePushHook",
        help = "Install git hook to automatically check files for style violations before push",
    ) {
    override fun run() {
        installGitHook(gitHookName = "pre-push") {
            """
            #!/bin/sh

            # <https://github.com/pinterest/ktlint> pre-push hook

            git diff --name-only -z HEAD "origin/${'$'}(git rev-parse --abbrev-ref HEAD)" -- '*.kt' '*.kts' | ktlint --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }
}
