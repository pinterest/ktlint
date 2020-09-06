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

    @Suppress("RemoveCurlyBracesFromTemplate")
    println("${s0}")
    @Suppress("RemoveCurlyBracesFromTemplate", "Unused")
    println("${s0}")
    @Suppress("RemoveCurlyBracesFromTemplate")
    val t = "${s0}"
}

class B(val k: String) {
    override fun toString(): String = "${super.toString()}, ${super.hashCode().toString()}, k=$k"

    @Suppress("RemoveCurlyBracesFromTemplate")
    val a
        get() = "${s0}"
}

@Suppress("RemoveCurlyBracesFromTemplate")
class C {
    override fun toString(): String = "${s0}"
}

// Ensure that suppression scope is as wide as it should be
class D {
    @Suppress("RemoveCurlyBracesFromTemplate")
    override fun toString(): String = "${s0}"

    fun test() = "${s0}"
}

@SuppressWarnings("RemoveCurlyBracesFromTemplate")
class E {
    override fun toString(): String = "${s0}"
}

class F {
    fun keyword() {
        println("${this}")
        println("${this@F}")
        println("${null}")
        println("${true}")
        println("${false}")
    }
}

// expect
// 2:29:Redundant "toString()" call in string template
// 3:28:Redundant "toString()" call in string template
// 6:15:Redundant curly braces
// 7:15:Redundant curly braces
// 28:79:Redundant "toString()" call in string template
// 45:20:Redundant curly braces
// 55:19:Redundant curly braces
