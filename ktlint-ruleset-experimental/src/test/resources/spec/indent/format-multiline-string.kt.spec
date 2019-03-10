fun f0() {
    println("${
    true
    }")

println("""${true}""")
println("""${true}""".trimIndent())
println("""
""".trimIndent())
println("""
""")
println("""
    text

        text
""".trimIndent())
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
    f("""${
true
    }
    text
_${
true
    }""", """text""")
}
