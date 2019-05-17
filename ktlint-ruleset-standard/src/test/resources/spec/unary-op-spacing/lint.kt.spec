import a.b.*
fun main() {
    c!!
    c !!
    i++
    i--
    i++--
    i --
    i ++
    i ++ --
    val y = + 1
    ! q
    !q
    - a
    + c
}

// expect
4:6:Unexpected spacing before "!!"
8:6:Unexpected spacing before "--"
9:6:Unexpected spacing before "++"
10:6:Unexpected spacing before "++"
10:9:Unexpected spacing before "--"
11:14:Unexpected spacing after "+"
12:6:Unexpected spacing after "!"
14:6:Unexpected spacing after "-"
15:6:Unexpected spacing after "+"
