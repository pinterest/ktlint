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

    var c = - /* comment */ (1 + 1)
    c = /* comment */- 1
    c = - 1/* comment */ + 1
}

// expect
// +4:6:Unexpected spacing in i ++
// +5:7:Unexpected spacing in ++\n    i
// +8:6:Unexpected spacing in i --
// +9:7:Unexpected spacing in -- i
// +11:6:Unexpected spacing in +  i
// +12:6:Unexpected spacing in -  i
// +15:10:Unexpected spacing in ! b
// +16:6:Unexpected spacing in ! b
// +19:23:Unexpected spacing in - 1
// +20:10:Unexpected spacing in - 1
