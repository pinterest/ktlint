class C {

    private val Any.className
        get() = this.javaClass.name
            .fn()

    private fun String.escape() =
        this.fn()
}
