public class A0 :
Comparable<*>,
        Appendable {
}

public class A1 :
    Comparable<*>,
    Appendable {
}

public class A2 : Comparable<*>,
    Appendable {
}

public class A3 :
    T<
        K,
        V
        > { // IDEA quirk
}

// expect
// 2:1:Unexpected indentation (0) (should be 4)
// 3:1:Unexpected indentation (8) (should be 4)
// 11:18:Missing newline after ":"
