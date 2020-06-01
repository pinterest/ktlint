fun funA(argA: String) =
    /* comment */
        /* comment */
/* comment */
    call(argA)

fun funB(argA: String) =
    /** comment */
        /** comment */
/** comment */
    call(argA)

fun main() {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
    // comment
        // comment
// comment
        override fun onLayoutChange(
        )
    })
}

//

/*
 *
 */

/**
 *
 */

// expect
// 3:1:Unexpected indentation (8) (should be 4)
// 9:1:Unexpected indentation (8) (should be 4)
// 15:1:Unexpected indentation (4) (should be 8)
