class C0 {
    fun good(): Int = funcReturningInt()
    fun bad(): Int {
        return funcReturningInt()
    }
    fun bad2() {
        funcReturningNothing()
    }
}

// expect
// 3:5:Single expression methods should use expression body (cannot be auto-corrected)
// 6:5:Single expression methods should use expression body (cannot be auto-corrected)
