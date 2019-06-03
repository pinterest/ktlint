class C0 {
    fun good(): Int = funcReturningInt()
    fun bad(): Int = funcReturningInt()
    fun bad2() = funcReturningNothing()
}

fun goodFreeFunc() = funcReturningNothing()

fun badFreeFunc() = funcReturningNothing()

fun returnStatementWithMethodChain(): String = StringBuilder().append(0).toString()

fun returnStatementWithConditions(): Int = if (true) 5 else 6

fun returnStatementInTryCatch(): Int = try {
        return 5
    } catch (e: Exception) {
        return 6
    }

fun ifStatement(): Int {
    if (true) return true
    return false
}
