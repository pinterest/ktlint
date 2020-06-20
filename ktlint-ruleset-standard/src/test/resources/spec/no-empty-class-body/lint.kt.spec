class C0 {}
data class DC0(val v: Any) {}
interface I0 {}
object O0 {}
object O00 { }

class C1
data class DC1(val v: Any)
interface I1
object O1

class C2 { /**/ }
data class DC2(val v: Any) { /**/ }
interface I2 { /**/ }
object O2 { /**/ }

val o = object : TypeReference<HashMap<String, String>>() {}

fun main() {}

class C3 {
    companion object {}
}

// expect
// 1:10:Unnecessary block ("{}")
// 2:28:Unnecessary block ("{}")
// 3:14:Unnecessary block ("{}")
// 4:11:Unnecessary block ("{}")
// 5:12:Unnecessary block ("{}")
