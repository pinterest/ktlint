@Deprecated("Foo")
public class ThisIsASampleClass :
	Comparable<*>,
	Appendable {
	val test =
		12

	@Deprecated("Foo")
	fun foo1(
		i1: Int,
		i2: Int,
		i3: Int
	): Int {
		when (i1) {
			is Number -> 0
			else -> 1
		}
		if (i2 > 0 &&
			i3 < 0
		) {
			return 2
		}
		return 0
	}

	private fun foo2(): Int {
// todo: something
		try {
			return foo1(
				12,
				13,
				14
			)
		} catch (e: Exception) {
			return 0
		} finally {
			if (true) {
				return 1
			} else {
				return 2
			}
		}
	}

	fun foo3() {
		Integer
			.parseInt("32").let {
				println("parsed $it")
			}
	}

	private val f =
		{ a: Int -> a * 2 }

	fun longMethod(
		@Named("param1") param1: Int,
		param2: String
	) {
		@Deprecated val foo =
			1
	}

	fun multilineMethod(
		foo: String,
		bar: String?,
		x: Int?
	) {
		foo.toUpperCase()
			.trim()
			.length
		val barLen =
			bar?.length() ?: x
				?: -1
		if (foo.length > 0 &&
			barLen > 0
		) {
			println("> 0")
		}
	}
}

@Deprecated
val bar = 1

enum class Enumeration {
	A, B
}

fun veryLongExpressionBodyMethod() =
	"abc"
