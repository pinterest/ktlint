// https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting

fun main() {
    foo(
       a,
       b,
       c
       )

    fn()
    fn(a, b, c)
}

// expect
// 5:1:indent:Unexpected indentation (7) (should be 8)
// 6:1:indent:Unexpected indentation (7) (should be 8)
// 7:1:indent:Unexpected indentation (7) (should be 8)
// 8:1:indent:Unexpected indentation (7) (should be 4)

