package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class StatementWrappingTest {
    private val statementWrappingAssertThat =
        KtLintAssertThat.assertThatRule { StatementWrapping() }

    @Test
    fun `Given function body starts at the same line with function lbrace`() {
        val code =
            """
            fun foo() { if (true) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(1, 13, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body ends at the same line with function rbrace`() {
        val code =
            """
            fun foo() {
                if (true) {
                    // do something
                } }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(4, 5, BEFORE_RBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body doesn't start or end with braces`() {
        val code =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function body is if else expression`() {
        val code =
            """
            fun foo() = if (true) {
                // do something
            } else {
                // do something else
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function body is number`() {
        val code =
            """
            fun foo() = 1
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function single line of body is aligns with both lbrace and rbrace`() {
        val code =
            """
            fun foo() { println("") }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println("")
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, AFTER_LBRACE_ERROR_MSG),
                LintViolation(1, 23, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function body is aligns with both lbrace and rbrace`() {
        val code =
            """
            fun foo() { if (true) {
                    // do something
                }}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, AFTER_LBRACE_ERROR_MSG),
                LintViolation(3, 5, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function doesn't have empty body`() {
        val code =
            """
            fun foo() {}
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given function doesn't have empty body with space`() {
        val code =
            """
            fun foo() { }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given variable initialisation is single line lambda`() {
        val code =
            """
            val foo = { /* no-op */ }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given variable initialisation is single line with code`() {
        val code =
            """
            fun bar() = 1
            val foo = { bar() }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given variable initialisation is multiline line lambda`() {
        val code =
            """
            val foo = {
                /* no-op */
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given variable initialisation is multiline line lambda has start aligned with lbrace`() {
        val code =
            """
            val foo = {when (true) {
                    true -> {

                    }
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = {
                when (true) {
                    true -> {

                    }
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(1, 12, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given variable initialisation is single line anonymous fun`() {
        val code =
            """
            val foo = fun() { /* no-op */ }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given variable initialisation is multi line anonymous fun`() {
        val code =
            """
            val foo = fun() {
                /* no-op */
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given when body first statement with statement on same line as that of lbrace`() {
        val code =
            """
            fun test(a: Int) {
                when (a) {1 -> {
                        println(a)
                    }

                    2 -> {
                    }
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                when (a) {
                    1 -> {
                        println(a)
                    }

                    2 -> {
                    }
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(2, 15, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given for body first statement with statement on same line as that of lbrace`() {
        val code =
            """
            fun test(a: Int) {
                for (i in 1..10) { println()
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                for (i in 1..10) {
                    println()
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(2, 24, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given for body last statement with statement on same line as that of rbrace`() {
        val code =
            """
            fun test(a: Int) {
                for (i in 1..10) {
                    println() }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                for (i in 1..10) {
                    println()
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(3, 17, BEFORE_RBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given while body fist statement with statement on same line as that of lbrace`() {
        val code =
            """
            fun test(a: Int) {
                while(a < Int.MAX) { println()
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                while(a < Int.MAX) {
                    println()
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(2, 26, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given while body last statement with statement on same line as that of rbrace`() {
        val code =
            """
            fun test(a: Int) {
                while(a < Int.MAX) {
                    println() }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                while(a < Int.MAX) {
                    println()
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(3, 17, BEFORE_RBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given do-while body first statement with statement on same line as that of lbrace`() {
        val code =
            """
            fun test(a: Int) {
                do { println()
                } while (a < Int.MAX)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                do {
                    println()
                } while (a < Int.MAX)
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(2, 10, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given do-while body last statement with statement on same line as that of rbrace`() {
        val code =
            """
            fun test(a: Int) {
                do {
                    println() } while (a < Int.MAX)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                do {
                    println()
                } while (a < Int.MAX)
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(3, 17, BEFORE_RBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given try-catch-finally body statements align with corresponding lbrace`() {
        val code =
            """
            fun test(a: Int) {
                try { println("1")
                    println("2")
                } catch (e: Exception) { println("1")
                    println("2")
                } finally {  println("1")
                    println("2")
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                try {
                    println("1")
                    println("2")
                } catch (e: Exception) {
                    println("1")
                    println("2")
                } finally {
                    println("1")
                    println("2")
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 11, AFTER_LBRACE_ERROR_MSG),
                LintViolation(4, 30, AFTER_LBRACE_ERROR_MSG),
                LintViolation(6, 18, AFTER_LBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given try-catch-finally body statements align with corresponding rbrace`() {
        val code =
            """
            fun test(a: Int) {
                try {
                    println("1")
                    println("2")  } catch (e: Exception) {
                    println("1")
                    println("2") } finally {
                    println("1")
                    println("2")}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                try {
                    println("1")
                    println("2")
                } catch (e: Exception) {
                    println("1")
                    println("2")
                } finally {
                    println("1")
                    println("2")
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 20, BEFORE_RBRACE_ERROR_MSG),
                LintViolation(6, 20, BEFORE_RBRACE_ERROR_MSG),
                LintViolation(8, 20, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given class block body statements align with corresponding rbrace and lbrace`() {
        val code =
            """
            class A { init {  }
                companion object { init {  }

                init {  }}
            init {  }}
            """.trimIndent()
        val formattedCode =
            """
            class A {
                init {  }
                companion object {
                    init {  }

                init {  }
                }
            init {  }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, AFTER_LBRACE_ERROR_MSG),
                LintViolation(2, 24, AFTER_LBRACE_ERROR_MSG),
                LintViolation(4, 13, BEFORE_RBRACE_ERROR_MSG),
                LintViolation(5, 9, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given init block body statements align with corresponding rbrace and lbrace`() {
        val code =
            """
            class A {
                init { println("start")

                    println("start")  }
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                init {
                    println("start")

                    println("start")
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 12, AFTER_LBRACE_ERROR_MSG),
                LintViolation(4, 24, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given nested misalignment in class and init block with lbrace on same line`() {
        val code =
            """
            class A {init { println("start")
                    println("start")
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                init {
                    println("start")
                    println("start")
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 10, AFTER_LBRACE_ERROR_MSG),
                LintViolation(1, 17, AFTER_LBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given object first line statement misaligned with brace`() {
        val code =
            """
            open class A
            fun test () {
                object : A() {val a = 1
                    val b = 1 }
            }
            """.trimIndent()
        val formattedCode =
            """
            open class A
            fun test () {
                object : A() {
                    val a = 1
                    val b = 1
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 19, AFTER_LBRACE_ERROR_MSG),
                LintViolation(4, 17, BEFORE_RBRACE_ERROR_MSG),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given lambda with parameters`() {
        val code =
            """
            fun interface Adder {
                fun add(a: Int, b: Int)
            }

            fun foo(a: Adder) {
                a.add(1, 2)
            }

            fun test() {
                foo { a, b ->
                    println(a + b)
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given lambda with parameters where first line of body aligns with the arrow`() {
        val code =
            """
            fun interface Adder {
                fun add(a: Int, b: Int)
            }

            fun foo(a: Adder) {
                a.add(1, 2)
            }

            fun test() {
                foo { a, b -> println(a + b)
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun interface Adder {
                fun add(a: Int, b: Int)
            }

            fun foo(a: Adder) {
                a.add(1, 2)
            }

            fun test() {
                foo { a, b ->
                    println(a + b)
                }
            }
            """.trimIndent()
        statementWrappingAssertThat(code)
            .hasLintViolation(10, 19, AFTER_LBRACE_ERROR_MSG)
            .isFormattedAs(formattedCode)
    }

    companion object {
        private const val AFTER_LBRACE_ERROR_MSG = "Expected new line after '{' of function body"
        private const val BEFORE_RBRACE_ERROR_MSG = "Expected new line before '}' of function body"
    }
}
