fun f0() {
    println("${
    true
    }")

println("""${true}""")
println("""
""")
println("""
""".trimIndent())
    f("""${
true
    }
    text
_${
true
    }""".trimIndent(), """text""")
}
