fun main() {
    val x = "${String::class.toString()}"
    println("${x}.hello")
    println("${x.toString()}.hello")
    println("${x}hello")
    println("${x.length}.hello")
    println("$x.hello")
}
