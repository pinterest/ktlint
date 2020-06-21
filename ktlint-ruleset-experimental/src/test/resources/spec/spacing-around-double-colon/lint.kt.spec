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
// 3:19:Unexpected spacing before "::"
// 4:21:Unexpected spacing after "::"
// 5:20:Unexpected spacing around "::"
// 6:21:Unexpected spacing after "::"
// 10:45:Unexpected spacing after "::"
// 11:42:Unexpected spacing before "::"
// 12:44:Unexpected spacing around "::"
// 16:45:Unexpected spacing after "::"
// 23:70:Unexpected spacing around "::"
// 28:11:Unexpected spacing after "::"
// 33:20:Unexpected spacing before "::"
