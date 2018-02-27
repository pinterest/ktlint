fun main() {
    println("${String::class.toString()}")
    println("""${Int::class.toString()}""")
    println("$s0")
    println("""$s1""")
    println("${s2}.hello")
    println("${s20}")

    println("${s3}hello")
    println("${s4.length}.hello")
    println("$s5.hello")

    println("$s.length is ${s.length}")
    println("${'$'}9.99")

    println("${h["x-forwarded-proto"] ?: "http"}")
    println("${if (diff > 0) "expanded" else if (diff < 0) "shrank" else "changed"}")
}

class B(val k: String) {
    override fun toString(): String = "${super.toString()}, ${super.hashCode().toString()}, k=$k"
}

// expect
// 2:29:Redundant "toString()" call in string template
// 3:28:Redundant "toString()" call in string template
// 6:15:Redundant curly braces
// 7:15:Redundant curly braces
// 21:79:Redundant "toString()" call in string template
