package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StatementWrappingRuleTest {
    private val statementWrappingRuleAssertThat =
        KtLintAssertThat.assertThatRule { StatementWrappingRule() }

    @Test
    fun `Given a function body with first statement at the same line as lbrace`() {
        val code =
            """
            fun foo() { doSomething()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolation(1, 13, "Missing newline after '{'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function body with last statement ending on same line as rbrace`() {
        val code =
            """
            fun foo() {
                doSomething() }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolation(2, 19, "Missing newline before '}'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function body with statement on separate line`() {
        val code =
            """
            fun foo() {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function with a single line body on same line as fun declaration`() {
        val code =
            """
            fun foo() { doSomething() }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Missing newline after '{'"),
                LintViolation(1, 27, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a multiline body and statements on same line as lbrace and rbrace`() {
        val code =
            """
            fun foo() { doSomething()
                doSomething() }
            fun foo() { if (true) {
                    doSomething()
                }}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                doSomething()
                doSomething()
            }
            fun foo() {
                if (true) {
                    doSomething()
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Missing newline after '{'"),
                LintViolation(2, 19, "Missing newline before '}'"),
                LintViolation(3, 13, "Missing newline after '{'"),
                LintViolation(5, 6, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a single line body with no more than one statement on same line a fun declaration`() {
        val code =
            """
            fun foo1() {}
            fun foo2() { }
            fun foo3() { /* no-op */ }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single line function literal`() {
        val code =
            """
            val foo1 = {}
            val foo2 = { }
            val foo3 = { /* no-op */ }
            val foo4 = { doSomething() }
            val foo5 = { bar -> doSomething(bar) }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a multiline line function literal with first statement on same line as lbrace`() {
        val code =
            """
            val foo = {doSomething()
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolation(1, 12, "Missing newline after '{'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline line function literal with last statement on same line as rbrace`() {
        val code =
            """
            val foo = {
                doSomething()}
            """.trimIndent()
        val formattedCode =
            """
            val foo = {
                doSomething()
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolation(2, 18, "Missing newline before '}'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a when-body with statements on same line as lbrace and rbrace`() {
        val code =
            """
            fun test(a: Int) {
                when (a) { 1 -> "foo"
                    2 -> "bar" }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                when (a) {
                    1 -> "foo"
                    2 -> "bar"
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 16, "Missing newline after '{'"),
                LintViolation(3, 20, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a for-body with statements on same line as lbrace and rbrace`() {
        val code =
            """
            fun test(a: Int) {
                for (i in 1..10) { doSomething()
                    doSomething() }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                for (i in 1..10) {
                    doSomething()
                    doSomething()
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 24, "Missing newline after '{'"),
                LintViolation(3, 23, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a while-body with statements on same line as lbrace and rbrace`() {
        val code =
            """
            fun test(a: Int) {
                while(a < Int.MAX) { doSomething()
                    doSomething() }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                while(a < Int.MAX) {
                    doSomething()
                    doSomething()
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 26, "Missing newline after '{'"),
                LintViolation(3, 23, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a do-while-body with statements on same line as lbrace and rbrace`() {
        val code =
            """
            fun test(a: Int) {
                do { doSomething()
                    doSomething() } while (a < Int.MAX)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                do {
                    doSomething()
                    doSomething()
                } while (a < Int.MAX)
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 10, "Missing newline after '{'"),
                LintViolation(3, 23, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-finally with statements on same line as lbrace`() {
        val code =
            """
            fun test(a: Int) {
                try { doSomething()
                } catch (e: Exception) { doSomething()
                } finally { doSomething()
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                try {
                    doSomething()
                } catch (e: Exception) {
                    doSomething()
                } finally {
                    doSomething()
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 11, "Missing newline after '{'"),
                LintViolation(3, 30, "Missing newline after '{'"),
                LintViolation(4, 17, "Missing newline after '{'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-finally with statements on same line as rbrace`() {
        val code =
            """
            fun test(a: Int) {
                try {
                    doSomething() } catch (e: Exception) {
                    doSomething() } finally {
                    doSomething() }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(a: Int) {
                try {
                    doSomething()
                } catch (e: Exception) {
                    doSomething()
                } finally {
                    doSomething()
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 23, "Missing newline before '}'"),
                LintViolation(4, 23, "Missing newline before '}'"),
                LintViolation(5, 23, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
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
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 23, "Missing newline before '}'"),
                LintViolation(6, 22, "Missing newline before '}'"),
                LintViolation(8, 21, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given class body with first statement on same line as lbrace`() {
        val code =
            """
            class Foo1 { init {
                    doSomething()
                }
            }
            class Foo2 {
                companion object { init {
                        doSomething()
                    }
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo1 {
                init {
                    doSomething()
                }
            }
            class Foo2 {
                companion object {
                    init {
                        doSomething()
                    }
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 14, "Missing newline after '{'"),
                LintViolation(6, 24, "Missing newline after '{'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given class body with first statement on same line as rbrace`() {
        val code =
            """
            class Foo1 {
                init {
                    doSomething()
                } }
            class Foo2 {
                companion object {
                    init {
                        doSomething() }
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo1 {
                init {
                    doSomething()
                }
            }
            class Foo2 {
                companion object {
                    init {
                        doSomething()
                    }
                }
            }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 7, "Missing newline before '}'"),
                LintViolation(8, 27, "Missing newline before '}'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given lambda with parameters and statement on separate line`() {
        val code =
            """
            val fooBar =
                foo { bar ->
                    doSomething(bar)
                }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given lambda with parameters and statement on same line as arrow`() {
        val code =
            """
            val fooBar =
                foo { bar -> doSomething(bar)
                }
            """.trimIndent()
        val formattedCode =
            """
            val fooBar =
                foo { bar ->
                    doSomething(bar)
                }
            """.trimIndent()
        statementWrappingRuleAssertThat(code)
            .hasLintViolation(2, 18, "Missing newline after '->'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2177 - Given a single line enumeration then do not wrap the values`() {
        val code =
            """
            enum class FooBar { FOO, BAR }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Issue 1078 - Given multiple expression separated with semi in a single line` {
        @Nested
        inner class `Given multiple variables` {
            @Test
            fun `Given two variables`() {
                val code =
                    """
                    fun foo() {
                        val bar1 = 3; val bar2 = 2
                        val fooBar1: String = ""; val fooBar2: () -> Unit = {  }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() {
                        val bar1 = 3
                        val bar2 = 2
                        val fooBar1: String = ""
                        val fooBar2: () -> Unit = {  }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(2, 18, "Missing newline after ';'"),
                        LintViolation(3, 30, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given two variables run without NoSemicolonsRule`() {
                val code =
                    """
                    fun foo() {
                        val bar1 = 3; val bar2 = 2
                        val fooBar1: String = ""; val fooBar2: () -> Unit = {  }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() {
                        val bar1 = 3;
                        val bar2 = 2
                        val fooBar1: String = "";
                        val fooBar2: () -> Unit = {  }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(2, 18, "Missing newline after ';'"),
                        LintViolation(3, 30, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given more than two variables`() {
                val code =
                    """
                    fun foo() {
                        val bar1 = 3; val bar2 = 2; val bar3 = 3; val bar4: () -> Unit = {  }; val bar4: String = "";
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() {
                        val bar1 = 3
                        val bar2 = 2
                        val bar3 = 3
                        val bar4: () -> Unit = {  }
                        val bar4: String = ""
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(2, 18, "Missing newline after ';'"),
                        LintViolation(2, 32, "Missing newline after ';'"),
                        LintViolation(2, 46, "Missing newline after ';'"),
                        LintViolation(2, 75, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given variables with comments`() {
                val code =
                    """
                    fun foo() {
                        val bar1 = 3; val bar2 = 2; // this is end comment
                        val bar1 = 3; /* block comment */ val bar2 = 2;
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun foo() {
                        val bar1 = 3
                        val bar2 = 2; // this is end comment
                        val bar1 = 3
                        /* block comment */ val bar2 = 2
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(2, 18, "Missing newline after ';'"),
                        LintViolation(3, 18, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }
        }

        @Nested
        inner class `Given multiple classes, functions and init blocks` {
            @Test
            fun `Given multiple function declaration`() {
                val code =
                    """
                    public fun foo1() {
                        // no-op
                    }; public fun foo2() {
                        // no-op
                    }; fun foo3() = 0

                    public fun foo4() = 1; public fun foo5() {
                        // no-op
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    public fun foo1() {
                        // no-op
                    }
                    public fun foo2() {
                        // no-op
                    }
                    fun foo3() = 0

                    public fun foo4() = 1
                    public fun foo5() {
                        // no-op
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(3, 3, "Missing newline after ';'"),
                        LintViolation(5, 3, "Missing newline after ';'"),
                        LintViolation(7, 23, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given multiple function declaration with comments`() {
                val code =
                    """
                    public fun foo1() {
                        // no-op
                    }; /* block comment */ public fun foo2() {
                        // no-op
                    }; fun foo3() = 0 // single line comment
                    """.trimIndent()
                val formattedCode =
                    """
                    public fun foo1() {
                        // no-op
                    }
                    /* block comment */ public fun foo2() {
                        // no-op
                    }
                    fun foo3() = 0 // single line comment
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(3, 3, "Missing newline after ';'"),
                        LintViolation(5, 3, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given multiple function invocations`() {
                val code =
                    """
                    class Bar {
                        public fun foo1() = 0
                        fun foo2() = 0
                        fun foo3(lambda: () -> Unit) = 0

                        init {
                            foo1(); foo3 {  }; foo2()
                        }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    class Bar {
                        public fun foo1() = 0
                        fun foo2() = 0
                        fun foo3(lambda: () -> Unit) = 0

                        init {
                            foo1()
                            foo3 {  }
                            foo2()
                        }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(7, 16, "Missing newline after ';'"),
                        LintViolation(7, 27, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiline class declaration`() {
                val code =
                    """
                    public class FooBar1 {

                    }; public class FooBar2 {

                    }

                    public class FooBar3; public class FooBar4
                    """.trimIndent()
                val formattedCode =
                    """
                    public class FooBar1 {

                    }
                    public class FooBar2 {

                    }

                    public class FooBar3
                    public class FooBar4
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(3, 3, "Missing newline after ';'"),
                        LintViolation(7, 22, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiline class declaration with comments`() {
                val code =
                    """
                    public class FooBar1 {

                    }; /* block comment */ public class FooBar2 {

                    }; public class FooBar2 {

                    } // single line comment
                    """.trimIndent()
                val formattedCode =
                    """
                    public class FooBar1 {

                    }
                    /* block comment */ public class FooBar2 {

                    }
                    public class FooBar2 {

                    } // single line comment
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(3, 3, "Missing newline after ';'"),
                        LintViolation(5, 3, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiple init block`() {
                val code =
                    """
                    public class Foo {
                        init {

                        };init {

                        }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    public class Foo {
                        init {

                        }
                        init {

                        }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolation(4, 7, "Missing newline after ';'")
                    .isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiple init block and variables with nested violations`() {
                val code =
                    """
                    public class Foo {
                        init {
                            val bar1 = 0; val bar2 = 0;
                        };init {
                            val bar3 = 0; val bar4 = 0;
                        }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    public class Foo {
                        init {
                            val bar1 = 0
                            val bar2 = 0
                        }
                        init {
                            val bar3 = 0
                            val bar4 = 0
                        }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(3, 22, "Missing newline after ';'"),
                        LintViolation(4, 7, "Missing newline after ';'"),
                        LintViolation(5, 22, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }
        }

        @Nested
        inner class `Given flow control statements` {
            @Test
            fun `Given a multiple for statements`() {
                val code =
                    """
                    fun test() {
                        for (i in 0..10) {
                            println(i)
                        }; for (i in 0..100) {
                            println(i)
                        }; for (i in 0..1000) {
                            println(i)
                        }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun test() {
                        for (i in 0..10) {
                            println(i)
                        }
                        for (i in 0..100) {
                            println(i)
                        }
                        for (i in 0..1000) {
                            println(i)
                        }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(4, 7, "Missing newline after ';'"),
                        LintViolation(6, 7, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiline while statements`() {
                val code =
                    """
                    fun test() {
                        while (System.currentTimeMillis() % 2 == 0L) {
                            println(System.currentTimeMillis())
                        }; while (Random(System.currentTimeMillis()).nextBoolean()) {
                            println(System.currentTimeMillis())
                        }
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun test() {
                        while (System.currentTimeMillis() % 2 == 0L) {
                            println(System.currentTimeMillis())
                        }
                        while (Random(System.currentTimeMillis()).nextBoolean()) {
                            println(System.currentTimeMillis())
                        }
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolation(4, 7, "Missing newline after ';'")
                    .isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiline do-while statements`() {
                val code =
                    """
                    fun test() {
                        while (System.currentTimeMillis() % 2 == 0L) {
                            println(System.currentTimeMillis())
                        }; do {
                            println(System.currentTimeMillis())
                        } while (System.currentTimeMillis() % 2 == 0L); do {
                            println(System.currentTimeMillis())
                        } while (System.currentTimeMillis() % 2 == 0L)
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun test() {
                        while (System.currentTimeMillis() % 2 == 0L) {
                            println(System.currentTimeMillis())
                        }
                        do {
                            println(System.currentTimeMillis())
                        } while (System.currentTimeMillis() % 2 == 0L)
                        do {
                            println(System.currentTimeMillis())
                        } while (System.currentTimeMillis() % 2 == 0L)
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(4, 7, "Missing newline after ';'"),
                        LintViolation(6, 52, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a multiline semi separated control flow with no body`() {
                val code =
                    """
                    fun test() {
                        for (i in 0..10); for (i in 0..100);while (System.currentTimeMillis() % 2 == 0L); while (Random(System.currentTimeMillis()).nextBoolean());
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    fun test() {
                        for (i in 0..10);
                        for (i in 0..100);
                        while (System.currentTimeMillis() % 2 == 0L);
                        while (Random(System.currentTimeMillis()).nextBoolean());
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolations(
                        LintViolation(2, 22, "Missing newline after ';'"),
                        LintViolation(2, 41, "Missing newline after ';'"),
                        LintViolation(2, 86, "Missing newline after ';'"),
                    ).isFormattedAs(formattedCode)
            }
        }

        @Test
        fun `Given a multiline semi separated import statement then wrap each expression to a new line`() {
            val code =
                """
                import java.util.ArrayList; import java.util.HashMap
                """.trimIndent()
            val formattedCode =
                """
                import java.util.ArrayList
                import java.util.HashMap
                """.trimIndent()
            statementWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { NoSemicolonsRule() }
                .hasLintViolation(1, 28, "Missing newline after ';'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a multiline semi separated with variables, flow controls and method calls`() {
            val code =
                """
                fun test() {
                    val a = 0; val b = 0; fun bar() {
                        // no-op
                    }; for(i in 0..10) {
                        println(i); println(i); a++; println(a)
                    }
                }
                """.trimIndent()
            val formattedCode =
                """
                fun test() {
                    val a = 0
                    val b = 0
                    fun bar() {
                        // no-op
                    }
                    for(i in 0..10) {
                        println(i)
                        println(i)
                        a++
                        println(a)
                    }
                }
                """.trimIndent()
            statementWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { NoSemicolonsRule() }
                .hasLintViolations(
                    LintViolation(2, 15, "Missing newline after ';'"),
                    LintViolation(2, 26, "Missing newline after ';'"),
                    LintViolation(4, 7, "Missing newline after ';'"),
                    LintViolation(5, 20, "Missing newline after ';'"),
                    LintViolation(5, 32, "Missing newline after ';'"),
                    LintViolation(5, 37, "Missing newline after ';'"),
                ).isFormattedAs(formattedCode)
        }

        @Nested
        inner class `Given enum class` {
            @Test
            fun `Given a enum without ending semi`() {
                val code =
                    """
                    enum class FOO1 { ONE, TWO, THREE }
                    enum class FOO2 {
                        ONE,
                        TWO,
                        THREE
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .hasNoLintViolations()
            }

            @Test
            fun `Given a enum with ending semi`() {
                val code =
                    """
                    enum class FOO1 { ONE, TWO, THREE; }
                    enum class FOO2 {
                        ONE, TWO, THREE;
                    }
                    enum class FOO3 {
                        ONE,
                        TWO,
                        THREE;
                    }
                    enum class FOO4 {
                        ONE,
                        TWO,
                        THREE,
                        ;
                    }
                    enum class FOO5 {
                        ONE,
                        TWO,
                        THREE,
                        ;
                        fun foo() = ""
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .hasNoLintViolations()
            }

            @Test
            fun `Given a enum with ending semi with comment`() {
                val code =
                    """
                    enum class FOO1 { ONE, TWO, THREE; /* with comment */ }
                    enum class FOO2 {
                        ONE,
                        TWO,
                        THREE, // single line comment
                        ; // last single line comment
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .hasNoLintViolations()
            }

            @Test
            fun `Given enum class with methods`() {
                val code =
                    """
                    enum class FOO {
                        A, B, C; fun test() = 0
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    enum class FOO {
                        A, B, C;
                        fun test() = 0
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolation(2, 13, "Missing newline after ';'")
                    .isFormattedAs(formattedCode)
            }
        }

        @Nested
        inner class `Given companion or object class` {
            @Test
            fun `Given a companion object with semicolon and variable`() {
                val code =
                    """
                    class Foo() {
                        companion object; private var toto: Boolean = false
                    }
                    """.trimIndent()
                val formattedCode =
                    """
                    class Foo() {
                        companion object;
                        private var toto: Boolean = false
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .addAdditionalRuleProvider { NoSemicolonsRule() }
                    .hasLintViolation(2, 22, "Missing newline after ';'")
                    .isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a companion object with semicolon and comment has not violation`() {
                val code =
                    """
                    class Foo() {
                        companion object; // single-line comment
                    }
                    """.trimIndent()
                statementWrappingRuleAssertThat(code)
                    .hasNoLintViolations()
            }
        }

        @Test
        fun `Given a single line block containing multiple statements then reformat block after wrapping the statement`() {
            val code =
                """
                val fooBar =
                    fooBar()
                        .map { foo(); bar() }
                """.trimIndent()
            val formattedCode =
                """
                val fooBar =
                    fooBar()
                        .map {
                            foo()
                            bar()
                        }
                """.trimIndent()
            statementWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { NoSemicolonsRule() }
                .addAdditionalRuleProvider { IndentationRule() }
                .hasLintViolations(
                    LintViolation(3, 16, "Missing newline after '{'"),
                    LintViolation(3, 22, "Missing newline after ';'"),
                    LintViolation(3, 29, "Missing newline before '}'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single line lambda expression containing multiple statements then reformat block after wrapping the statement`() {
            val code =
                """
                val fooBar =
                    fooBar()
                        .map { foo, bar -> print(foo); print(bar) }
                """.trimIndent()
            val formattedCode =
                """
                val fooBar =
                    fooBar()
                        .map { foo, bar ->
                            print(foo)
                            print(bar)
                        }
                """.trimIndent()
            statementWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { NoSemicolonsRule() }
                .addAdditionalRuleProvider { IndentationRule() }
                .hasLintViolations(
                    LintViolation(3, 28, "Missing newline after '->'"),
                    LintViolation(3, 39, "Missing newline after ';'"),
                    LintViolation(3, 51, "Missing newline before '}'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single line when entry block containing multiple statements then reformat block after wrapping the statement`() {
            val code =
                """
                val foo =
                    when (value) {
                        0 -> { foo(); true }
                        else -> { bar(); false }
                    }
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    when (value) {
                        0 -> {
                            foo()
                            true
                        }
                        else -> {
                            bar()
                            false
                        }
                    }
                """.trimIndent()
            statementWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { NoSemicolonsRule() }
                .addAdditionalRuleProvider { IndentationRule() }
                .hasLintViolations(
                    LintViolation(3, 16, "Missing newline after '{'"),
                    LintViolation(3, 22, "Missing newline after ';'"),
                    LintViolation(3, 28, "Missing newline before '}'"),
                    LintViolation(4, 19, "Missing newline after '{'"),
                    LintViolation(4, 25, "Missing newline after ';'"),
                    LintViolation(4, 32, "Missing newline before '}'"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a single line enumeration class preceded by a comment`() {
        val code =
            """
            /**
             * Some comment
             */
            enum class Foobar { FOO, BAR }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single line enumeration class preceded by one or more annotations`() {
        val code =
            """
            @FooBar
            enum class Foobar { FOO, BAR }
            """.trimIndent()
        statementWrappingRuleAssertThat(code).hasNoLintViolations()
    }
}
