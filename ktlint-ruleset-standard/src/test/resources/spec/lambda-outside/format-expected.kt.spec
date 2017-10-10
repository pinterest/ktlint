fun main() {
    f0 { res -> println(res) }
    f1("") { res -> println(res) }
    f2({ res -> println(res) }, { err -> println(err) })

    f3 { res -> println(res) }
    f4("") { res -> println(res) }
    f5({ res -> println(res) }, { err -> println(err) })
}
