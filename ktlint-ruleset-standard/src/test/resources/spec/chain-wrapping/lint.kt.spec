fun main() {
    val anchor = owner.firstChild!!.
        siblings(forward = true).
        dropWhile { it is PsiComment || it is PsiWhiteSpace }
    val s = foo() ?:
        bar
    val s = foo()?.
        bar
    val s = "foo"
        + "bar"
    val s = true
        && false
    val s = b.equals(o.b)
        && g == o.g
    val d = 1 +
        -1
    fn(
        *typedArray<EventListener>(),
        -0,
        *typedArray<EventListener>()
    )
}

// expect
// 2:36:Line must not end with "."
// 3:33:Line must not end with "."
// 5:19:Line must not end with "?:"
// 7:18:Line must not end with "?."
// 10:9:Line must not begin with "+"
// 12:9:Line must not begin with "&&"
// 14:9:Line must not begin with "&&"
