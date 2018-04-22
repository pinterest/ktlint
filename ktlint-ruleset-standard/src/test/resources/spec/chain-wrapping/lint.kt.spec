fun main() {
    val anchor = owner.firstChild!!.
        siblings(forward = true).
        dropWhile { it is PsiComment || it is PsiWhiteSpace }
    val s = foo() ?:
        bar
    val s = foo()?.
        bar
    val d = 1
        + 1
    val s = true
        && false
    val s = b.equals(o.b)
        && g == o.g
    val s = ((1 + 2)
        / 3)
    val d = 1 +
        -1
    val d = 1
        + -1
    val d = (1
        + 1)
    fn(1,
    -1)
    fn(
        *typedArray<EventListener>(),
        -0,
        *typedArray<EventListener>()
    )
}

/**
 * @see KtLint.EDITOR_CONFIG_USER_DATA_KEY
 * @see KtLint.ANDROID_USER_DATA_KEY
 */
fun get(key: String): String?

// expect
// 2:36:Line must not end with "."
// 3:33:Line must not end with "."
// 5:19:Line must not end with "?:"
// 7:18:Line must not end with "?."
// 12:9:Line must not begin with "&&"
// 14:9:Line must not begin with "&&"
// 16:9:Line must not begin with "/"
// 22:9:Line must not begin with "+"
