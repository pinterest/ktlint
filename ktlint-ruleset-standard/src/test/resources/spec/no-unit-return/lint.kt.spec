fun f1() {}
fun f2(): Unit {}
fun f2(): Unit = start()
fun f2_(): Unit /**/
    = start()
fun f3(): String = ""

// expect
// 2:11:Unnecessary "Unit" return type
