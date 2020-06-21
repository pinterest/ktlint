// https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting

fun main() {
    fn(a,
       b,
       c)

    fn()
    fn(a, b, c)
}

// expect
// 4:8:Missing newline after "("
// 5:1:Unexpected indentation (7) (should be 8)
// 6:1:Unexpected indentation (7) (should be 8)
// 6:8:Missing newline before ")"

