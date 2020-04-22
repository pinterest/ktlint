fun main() {
    val a = AClass::class
    val b = BClass::class
    val c = CClass::class
    val d = DClass::class
    val e = EClass::class
    val f = FClass::class

    fun isOdd(s: String) = s == "brillig" || s == "slithy" || s == "tove"
    val predicateA: (String) -> Boolean = ::isOdd
    val predicateB: (String) -> Boolean = ::isOdd
    val predicateC: (String) -> Boolean = ::isOdd
    val predicateD: (String) -> Boolean = ::isOdd
    val predicateE: (String) -> Boolean =
        ::isOdd
    val predicateF: (String) -> Boolean = ::isOdd

    if (true == ::isOdd.invoke("")) {
        // do stuff
    }

    val isEmptyStringList: List<String>.() -> Boolean = List<String>::isEmpty
    val isNotEmptyStringList: List<String>.() -> Boolean = List<String>::isNotEmpty

    function(::Foo)
    function(
        ::Foo
    )

    items.filter(::isEven)
        .map(String::length)
}
