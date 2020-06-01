class C {
    // KtNodeTypes
    val CLASS: IElementType = KtNodeTypes.CLASS
    val FUN: IElementType = KtNodeTypes.FUN
}

fun f() {
    val x = "a" +
        "b2"
    val x =
        "a" +
            "b2"

    val x = paths.flatMap { dir ->
        "hello"
    } + f0(
        "there"
    ) + f1(
        "sssss"
    )

    fun Exception.toLintError(): LintError = this.let { e ->
        //
    }

    val tokenSet = TokenSet.create(FOR_KEYWORD, IF_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
        TRY_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD, WHEN_KEYWORD)

    val x = when (1) {
        else -> ""
    }

    val barLen =
        x ?: y
        ?: -1
    val barLen =
        x
            ?: y
            ?: -1

    val barLen =
        x ?: (
            true ||
                false
            )
        ?: -1
    val barLen =
        (
            true ||
                false
            ) ?: (
            true ||
                false
            )
        ?: -1

    val barLen =
        bar?.length() ?: x
        ?: -1

    val barLen =
        bar?.length() ?: x
        ?: -1
        ?: -1

    val barLen =
        (bar?.length() ?: x)
            ?: -1

    val barLen =
        bar?.length() || x
            ?: -1

    val barLen =
        bar?.length()
            ?: -1

    val userData = (
        EditorConfig.of(File(editorConfigPath).canonicalPath)
            ?.onlyIf({ debug }) { printEditorConfigChain(it) }
            ?: emptyMap<String, String>()
        ) + cliUserData

    val CONFIG_COMPACT = // comment
        """
        {
        }
        """.trimIndent()
}
