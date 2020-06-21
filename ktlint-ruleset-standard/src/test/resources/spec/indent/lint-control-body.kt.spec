fun f() {
    if (true)
        println()
    else
        println()

    if (true) {
        println()
    } else {
        println()
    }

    if (true) (
        1
        ) else ( // IDEA quirk
        0
        ) // IDEA quirk

    while (true)
        println()

    while (true) {
        println()
    }
}

// expect
