package io.github.ktlint.core.cli.internal

internal class GitPrePushHookSubCommand :
    GitHookCliktCommand(
        name = "installGitPrePushHook",
        helpText = "Install git hook to automatically check files for style violations before push",
    ) {
    override fun run() {
        installGitHook(gitHookName = "pre-push") {
            """
            #!/bin/sh

            # <https://github.com/ktlint/ktlint> pre-push hook

            git diff --name-only -z HEAD "origin/${'$'}(git rev-parse --abbrev-ref HEAD)" -- '*.kt' '*.kts' | ktlint --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }
}
