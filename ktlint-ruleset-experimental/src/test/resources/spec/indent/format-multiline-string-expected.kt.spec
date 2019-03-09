fun f0() {
    println(
        "${
        true
        }"
    )

    println("""${true}""")
    f(
        """${
        true
        }
    text
_${
        true
        }""",
        """text"""
    )
}
