fun main() {
    super ()
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
// 2:10:Unexpected spacing before "("
// 3:14:Unexpected spacing after "("
// 3:26:Unexpected spacing before ")"
// 4:8:Unexpected spacing after "("
// 5:7:Unexpected spacing before "("
// 6:8:Unexpected spacing after "("
// 7:16:Unexpected spacing before "("
// 8:8:Unexpected spacing after "("
