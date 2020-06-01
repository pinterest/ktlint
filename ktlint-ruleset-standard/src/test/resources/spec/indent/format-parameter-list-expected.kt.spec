class C (
    val a: Int, val b: Int,
    val e: (
        r: Int
    ) -> Unit,
    val c: Int, val d: Int
) {

    fun f(
        a: Int, b: Int,
        e: (
            r: Int
        ) -> Unit,
        c: Int, d: Int
    ) {

    }
}

class TestClass(
    @Id @NotNull
    @Column(
        nullable = false
    )
    val id: String
)
