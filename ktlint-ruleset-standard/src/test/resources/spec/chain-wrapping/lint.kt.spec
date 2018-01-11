fun main() {
    val anchor = owner.firstChild!!.
        siblings(forward = true).
        dropWhile { it is PsiComment || it is PsiWhiteSpace }
    val s = foo() ?:
        bar
    val s = foo()?.
        bar
//    val s = "foo"
//        + "bar"
}

// expect
// 2:36:Line must not end with "."
// 3:33:Line must not end with "."
// 5:19:Line must not end with "?:"
// 7:18:Line must not end with "?."
