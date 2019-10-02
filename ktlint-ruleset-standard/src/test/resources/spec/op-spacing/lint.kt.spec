import a.b.*
fun main() {
    val v = 0 - 1 * 2
    val v1 = 0-1*2
    val v2 = -0 - 1
    val v3 = v * 2
    fn(1 + 1 - 1 * 1 / 1)
    fn(1+1-1*1/1)
    fn(v1+1*v2)
    run { v1*2 }
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
    call(1, *v, 2)
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
// 8:9:Missing spacing around "+"
// 8:11:Missing spacing around "-"
// 8:13:Missing spacing around "*"
// 8:15:Missing spacing around "/"
// 9:10:Missing spacing around "+"
// 9:12:Missing spacing around "*"
// 10:13:Missing spacing around "*"
// 18:10:Missing spacing before "="
