package com.omgsooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo.long

import com.omg.sooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo.superduper.long
import com.omg.kindof.long // ktlint-disable fake-rule-with-a-super-duper-long-name
import com.omg.kindof.long // some other comment that is really long why is this here?

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
// 5:1:Exceeded max line length (80) (cannot be auto-corrected)
// 10:1:Exceeded max line length (80) (cannot be auto-corrected)
// 11:1:Exceeded max line length (80) (cannot be auto-corrected)
// 12:1:Exceeded max line length (80) (cannot be auto-corrected)
// 14:1:Exceeded max line length (80) (cannot be auto-corrected)
