val a =
    1 + 2 +
        3 +
        4
val b =
    true &&
    (false || false) ||
        false
val c = 1

fun main() {
    if (
        listOf<Any>()
            .toString()
            .isEmpty() &&
        false ||
        (
            true ||
                false
            ) ||
        false
    ) {
        println("hello")
    }
}

// expect
// 7:1:Unexpected indentation (4) (should be 8)
