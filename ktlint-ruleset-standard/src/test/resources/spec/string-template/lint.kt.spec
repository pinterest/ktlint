fun main() {
    println("${String::class.toString()}")
    println("""${Int::class.toString()}""")
    println("$s0")
    println("""$s1""")
    println("${s2}.hello")

    println("${s3}hello")
    println("${s4.length}.hello")
    println("$s5.hello")

    println("$s.length is ${s.length}")
    println("${'$'}9.99")
}

// expect
// 2:29:Redundant 'toString()' call in string template
// 3:28:Redundant 'toString()' call in string template
// 6:15:Redundant curly braces
