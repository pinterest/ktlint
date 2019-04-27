fun f1() =
"""

"""

fun f2() =
    """

    """

fun f3() =
    "hello ${
    f()
        .a()
        .b()
    } there"

fun f4() = "${
true
}"

// expect
