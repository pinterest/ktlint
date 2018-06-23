fun main() {
    val a = ( (1 + 2) / 3 )
    fn( (1 + 2) / 3)
    fn ((1 + 2) / 3)
    fn( )
    @Deprecated ("")
    fn(
    )

    mapOf(
      "key" to (v ?: "")
    )

    val b = ((1 + 2) / 3)
    val c = (
      (1 + 2) / 3
    )
    fn((1 + 2) / 3)
}

// expect
// 2:14:Unexpected spacing after "("
// 2:26:Unexpected spacing before ")"
// 3:8:Unexpected spacing after "("
// 4:7:Unexpected spacing before "("
// 5:8:Unexpected spacing after "("
// 6:16:Unexpected spacing before "("
// 7:8:Unexpected spacing after "("
