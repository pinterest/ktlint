class A1 : Spek({

    describe("") {
    }
})

class A2 : X, Spek({

    describe("") {
    }
})

class A3 : X, Spek({

    describe("") {
    }
}), Y

class A4 : Spek1({

    describe("") {
    }
}), Spek2({

    describe("") {
    }
})

class A5 :
    Spek({

        describe("") {
        }
    })

class A6 :
    T<
        K,
        V
    >, // IDEA quirk
    Z({

    })

class MyClass(
    thisIsAParameter: ThisIsTheParameterClass
) : AnotherClassName<SomeClass.AnInterfaceName>(thisIsAParameter),
    YetAnotherInterfaceWeDeriveFrom {
    val x = 1
    val y = 2
}

class AndroidModuleDependency()
    : ModuleDependency(name, methodToCall, method)

class AndroidModuleDependency()
    : ModuleDependency(name, methodToCall, method),
    ModuleDependency(name, methodToCall, method)

// https://github.com/pinterest/ktlint/issues/518
enum class Color(val displayName: String, val value: Int) {
    RED(
        displayName = "Red",
        value = 1
    ),
    BLUE(
        displayName = "Blue",
        value = 2
    );
}
