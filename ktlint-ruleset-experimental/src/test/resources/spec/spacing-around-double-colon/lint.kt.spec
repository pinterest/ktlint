fun main() {
    val a = AClass::class
    val b = BClass ::class
    val c = CClass:: class
    val d = DClass :: class
    val e = EClass::
        class

    fun isOdd(s: String) = s == "brillig" || s == "slithy" || s == "tove"
    val predicateA: (String) -> Boolean = :: isOdd
    val predicateB: (String) -> Boolean =  ::isOdd
    val predicateC: (String) -> Boolean =  :: isOdd
    val predicateD: (String) -> Boolean = ::isOdd
    val predicateE: (String) -> Boolean =
        ::isOdd
    val predicateF: (String) -> Boolean = ::
        isOdd

    if (true == ::isOdd.invoke("")) {
        // do stuff
    }

    val isEmptyStringList: List<String>.() -> Boolean = List<String> :: isEmpty
    val isNotEmptyStringList: List<String>.() -> Boolean = List<String>::isNotEmpty

    function(::Foo)
    function(
        ::
        Foo
    )

    items.filter(::isEven)
        .map(String ::length)
}

// expect
// 3:19:double-colon-spacing:Unexpected spacing before "::"
// 4:21:double-colon-spacing:Unexpected spacing after "::"
// 5:20:double-colon-spacing:Unexpected spacing around "::"
// 6:21:double-colon-spacing:Unexpected spacing after "::"
// 10:45:double-colon-spacing:Unexpected spacing after "::"
// 11:42:double-colon-spacing:Unexpected spacing before "::"
// 12:44:double-colon-spacing:Unexpected spacing around "::"
// 16:45:double-colon-spacing:Unexpected spacing after "::"
// 23:70:double-colon-spacing:Unexpected spacing around "::"
// 28:11:double-colon-spacing:Unexpected spacing after "::"
// 33:20:double-colon-spacing:Unexpected spacing before "::"
