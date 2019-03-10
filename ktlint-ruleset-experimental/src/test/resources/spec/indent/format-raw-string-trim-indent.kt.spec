fun f() {
println("""${true}""".trimIndent())
println("""
""".trimIndent())
println("""
    text

        text
""".trimIndent().toByteArray())
println("""
    text

        text
 """.trimIndent())
println("""
    text

        text
_""".trimIndent())
    println(
        """
    text ""

        text
    """.trimIndent(),
        ""
    )
    write(fs.getPath("/projects/.editorconfig"), """
    root = true
    [*]
    end_of_line = lf
""".trimIndent().toByteArray())
            SpacingAroundKeywordRule().format( // string below is tab-indented
                """
            var x: String
			    get () {
				    return ""
			    }
			    private set (value) {
				    x = value
			    }
            """.trimIndent()
            )
}
