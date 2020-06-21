typealias F = (
    v: String
) -> Unit

fun main() {
    f({ v -> d(
    1
    )})
    f({ v -> x
        .f()
    })
    when {
        // comment
        true -> {

        }
    }
    when {
        1, // first element
        2 // second element
        -> true
    }
    when {
        1,
        2 -> true
    }
    foo.func {
        param1, param2 ->
            doSomething()
            doSomething2()
        }
}
