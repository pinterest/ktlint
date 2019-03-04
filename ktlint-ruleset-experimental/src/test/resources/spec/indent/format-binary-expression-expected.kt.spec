fun f() {
    x(
        paths.flatMap { dir ->
            "hello"
        } + f0(
            "there"
        ) + f1(
            "sssss"
        )
    )

    y(
        ""
            + ""
            + f2(
                "" // IDEA quirk (ignored)
            )
    )

    val x =
        "a" to
            "x" +
            "x" +
            "x"
    val x =
        "b" +
            "x" +
            "x" +
            "x"
    val x =
        "a" to
            "x"
                .plus("y")
                .plus("y")
                .plus("y")
}

object Y {
    @Option(
        names = arrayOf("--install-git-pre-commit-hook"),
        description = arrayOf(
            "A" +
                "B"
        )
    )
    private val DEPRECATED_FLAGS = mapOf(
        "--ruleset-repository" to
            "--repository" +
            "aaa" +
            "ccc",
        "--reporter-repository" to
            "--repository",
        "--ruleset-update" to
            "--repository-update",
        "--reporter-update" to
            "--repository-update"
    )
}
