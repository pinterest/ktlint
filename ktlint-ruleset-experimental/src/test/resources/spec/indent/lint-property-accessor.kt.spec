class A {
    val b: Boolean
    get() = true

    var value: String = ""
        get() = ""
        set(v: String) { field = v }

    var valueMultiLine: String = ""
        get() {
            return ""
        }
        // comment
        set(v: String) {
            field = v
        }

    val isEmpty: Boolean
        get() = this.size == 0

    var counter = 0 // comment
        set(value) { // comment
            if (value >= 0) field = value
        }

    var setterVisibility: String = "abc"
        private set

    var setterWithAnnotation: Any? = null
        @Inject set
}

// expect
// 3:1:Unexpected indentation (4) (should be 8)
