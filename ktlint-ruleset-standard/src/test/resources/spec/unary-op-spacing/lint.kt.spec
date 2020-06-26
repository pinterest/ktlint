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
