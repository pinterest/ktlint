class C0 {
    fun good(): Int = funcReturningInt()
    fun bad(): Int = funcReturningInt()
    fun bad2() = funcReturningNothing()
}

fun goodFreeFunc() = funcReturningNothing()

fun badFreeFunc() = funcReturningNothing()
