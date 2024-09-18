package com.pinterest.ktlint.cli.internal

internal class GitPreCommitHookSubCommand :
    GitHookCliktCommand(
        name = "installGitPreCommitHook",
        helpText = "Install git hook to automatically check files for style violations on commit",
    ) {
    override fun run() {
        installGitHook(gitHookName = "pre-commit") {
            """
            #!/bin/sh

            # <https://github.com/pinterest/ktlint> pre-commit hook

            git diff --name-only -z --cached --relative -- '*.kt' '*.kts' | ktlint --relative --patterns-from-stdin=''
            """.trimIndent().toByteArray()
        }
    }
}
