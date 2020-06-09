fun main() {
  var a: Map< Int, String> = mapOf()
  var b: Map<Int, String > = mapOf()
  var c: Map <Int, String> = mapOf()

  var nested: Map<Int, List <   String >    > = mapOf()
}

public class AngleTest< B : String  > {}

// expect
// 2:14:Unexpected spacing after "<"
// 3:25:Unexpected spacing before ">"
// 4:13:Unexpected spacing before "<"
// 6:28:Unexpected spacing before "<"
// 6:30:Unexpected spacing after "<"
// 6:39:Unexpected spacing before ">"
// 6:41:Unexpected spacing before ">"
// 9:24:Unexpected spacing after "<"
// 9:35:Unexpected spacing before ">"
