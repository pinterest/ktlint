package com.omg.sooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo.long
import com.omg.soooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo.long
// http://______________________________________________________________________.
fun main() {
    // comment padded with spaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaace
    println("__________________________________________________________________")
    println("") // too looooooooooooooooooooooooooooooooooooooooooooooooooooooong
    println("8_____________${"$"}_____________________________________\n_______")
    println(
      """10________________${"$"}_____________________________________________"""
    )
    println(
      """
      14___________________${"$"}_____________________________________________"""
    )
    val long = """
17______________________________________________________________________________.

19
    """
    println(
      """
23______________________________________________________________________________.$v
24______________________________________________________________________________.${f()}
      """
    )
}

/**
 * "https://www.google.com/search?q=ktlint&rlz=1C5CHFA_enMD736MD737&oq=ktlint+&aqs=chrome..69i57j69i60l4j69i59.1286j0j4&sourceid=chrome&ie=UTF-8"
 */

// expect
// 6:1:Exceeded max line length (80)
// 7:1:Exceeded max line length (80)
// 8:1:Exceeded max line length (80)
// 10:1:Exceeded max line length (80)
