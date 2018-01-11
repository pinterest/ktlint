import a.b.*
fun main() {
    val v = 0 - 1 * 2
    val v1 = 0-1*2
    val v2 = -0 - 1
    val v3 = v * 2
    i++
    val y = +1
    var x = 1 in 3..4
    val b = 1 < 2
    fun(a = true)
    val res = ArrayList<LintError>()
    fn(*arrayOfNulls<Any>(0 * 1))
    val a= ""
    d *= 1
    call(*v)
    open class A<T> {
        open fun x() {}
    }
    class B<T> : A<T>() {
        override fun x() = super<A>.x()
    }
}

// expect
// 4:15:Missing spacing around "-"
// 4:17:Missing spacing around "*"
// 14:10:Missing spacing before "="
