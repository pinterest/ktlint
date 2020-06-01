val b =
    nullableList
   .find { !it.empty() }
     ?.map { x + 2 }
    ?.filter { true }
val a =
    listOf(listOf(1, 2, 3))
        .map {
            it
                .map { it + 1 }
              .filter { it % 2 == 0 }
        }
        .reduce { acc, curr -> acc + curr }
        .toString()
val c = 1

// expect
// 3:1:Unexpected indentation (3) (should be 8)
// 4:1:Unexpected indentation (5) (should be 8)
// 5:1:Unexpected indentation (4) (should be 8)
// 11:1:Unexpected indentation (14) (should be 16)
