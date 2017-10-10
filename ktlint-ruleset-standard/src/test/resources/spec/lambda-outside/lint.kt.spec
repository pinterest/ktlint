fun main() {
    f0({ res -> println(res) })
    f1("", { res -> println(res) })
    f2({ res -> println(res) }) { err -> println(err) }

    f3 { res -> println(res) }
    f4("") { res -> println(res) }
    f5({ res -> println(res) }, { err -> println(err) })
    args.then({ _, _ -> "" }, v = "") { _, _ -> "" }
}

// expect
// 2:8:Lambda must be outside of parentheses
// 3:12:Lambda must be outside of parentheses
// 4:33:Lambda must be enclosed in parentheses
