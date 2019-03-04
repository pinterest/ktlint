fun main() {
    val v =
        listOf<Any>()
            .toString()
            .isEmpty() &&
            false ||
            (
                true ||
                    false ||
                    listOf<Any>()
                        .toString()
                        .isEmpty()
                ) ||
            false

    while (
        listOf<Any>()
            .toString()
            .isEmpty() &&
        false ||
        (
            true ||
                false ||
                listOf<Any>()
                    .toString()
                    .isEmpty()
            ) ||
        false
    ) {
        println("hello")
    }

    if (true && // IDEA quirk
        true
    ) {

    }

    if (
        listOf<Any>()
            .toString()
            .isEmpty() &&
        false ||
        (
            true ||
                false ||
                listOf<Any>()
                    .toString()
                    .isEmpty()
            ) ||
        false
    ) {
        println("hello")
    }
}

// expect
