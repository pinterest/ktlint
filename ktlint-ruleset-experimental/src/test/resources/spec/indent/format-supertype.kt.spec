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
