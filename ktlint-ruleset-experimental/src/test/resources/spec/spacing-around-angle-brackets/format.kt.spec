// TYPE_ARGUMENT_LIST
fun main() {
  var a: Map< Int, String> = mapOf()
  var b: Map<Int, String > = mapOf()
  var c: Map <Int, String> = mapOf()

  var d: Map<


    Int, String

    > = mapOf()

  // Indentation would be fixed by another rule
  var e: Map<
                Int,
                String
  > = mapOf()

  var nested: Map<
    Int,
    List <   String >
    > = mapOf()

  // Multiple spacing would be fixed by another rule
  fun    <T> compare(other: T) {}
}

// TYPE_PARAMETER_LIST
public class AngleTest< B : String  > {
    var a = 'str'
}

public class AngleTest<

    B : String,
    C : Map <
        Int, List< String >
        >

    > {
    var a = 'str'
}
