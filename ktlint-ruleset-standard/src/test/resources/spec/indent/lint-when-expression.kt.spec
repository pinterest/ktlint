fun main() {
    val v = when (1) {
        1 ->
            true
        2 -> false
        3 ->
        true
        4 -> false ||
            true
        else -> {
            true
        }
    }
    val v2 = 0 + 1 + when {
        else -> 2 + 3
    }
    when {
        true -> 0 + 1 + when {
            else -> 2 + 3
        }
    }
}

// expect
// 7:1:Unexpected indentation (8) (should be 12)
// 8:12:Missing newline after "->"
