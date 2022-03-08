public class A0 :
Comparable<*>,
        Appendable {
}

public class A1 :
    Comparable<*>,
    Appendable {
}

public class A3 :
    T<
        K,
        V
        > { // IDEA quirk
}

// expect
// 2:1:indent:Unexpected indentation (0) (should be 4)
// 3:1:indent:Unexpected indentation (8) (should be 4)
