fun main() {
  (1 ..12 step 2).last == 11
  (1.. 12 step 2).last == 11
  (1 .. 12 step 2).last == 11

  (1..12 step 2).last == 11
  for (i in 1..4) print(i)
}

// expect
// 2:5:Unexpected spacing before ".."
// 3:7:Unexpected spacing after ".."
// 4:6:Unexpected spacing around ".."
