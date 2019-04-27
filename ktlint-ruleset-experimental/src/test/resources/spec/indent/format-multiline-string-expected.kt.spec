fun f0() {
    println(
        "${
        true
        }"
    )

    println("""${true}""")
    println(
        """
"""
    )
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
