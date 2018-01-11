fun main() {
    val anchor = owner.firstChild!!.
        siblings(forward = true).
        dropWhile { it is PsiComment || it is PsiWhiteSpace }
    val s = foo() ?:
        bar
    val s = foo()?.
        bar
}
