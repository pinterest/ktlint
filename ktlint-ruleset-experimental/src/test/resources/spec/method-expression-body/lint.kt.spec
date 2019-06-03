class C0 {
    fun good(): Int = funcReturningInt()
    fun bad(): Int {
        return funcReturningInt()
    }
    fun bad2() {
        funcReturningNothing()
    }
}

fun goodFreeFunc() = funcReturningNothing()

fun badFreeFunc() {
    funcReturningNothing()
}

fun returnStatementWithMethodChain(): String {
    return StringBuilder().append(0).toString()
}

fun returnStatementWithConditions(): Int {
    return if (true) 5 else 6
}

fun returnStatementInTryCatch(): Int {
    return try {
        return 5
    } catch (e: Exception) {
        return 6
    }
}

fun ifStatement(): Int {
    if (true) return true
    return false
}

// expect
// 3:5:Single expression methods should use an expression body
// 6:5:Single expression methods should use an expression body
// 13:1:Single expression methods should use an expression body
// 17:1:Single expression methods should use an expression body
// 21:1:Single expression methods should use an expression body
