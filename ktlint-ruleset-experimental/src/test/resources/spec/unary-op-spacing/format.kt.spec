fun main() {
    var i = 1

    i ++
    ++
    i

    i --
    -- i

    +  i
    -  i

    var b = false
    b = ! b
    ! b

    var e = - 1 in 1..10
    var f = - (-- i) + 1 + 1
    f = - (++ i) + 1 + 1
    var g = + 1 - 1 + (- 1)
    var h = - 1 in - 1..+ 10
    if (- 1 < - f && + f > - 10) {
        f += -1 + 2 + - 3 - 4 + (-4)
    }

    val el = node.psi.findElementAt(offset + line.length - 1) !!.node

    var c = - /* comment */ (1 + 1)
    c = /* comment */- 1
    c = - 1/* comment */ + 1
}
