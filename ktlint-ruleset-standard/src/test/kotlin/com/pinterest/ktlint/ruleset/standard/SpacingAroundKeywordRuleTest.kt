package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class SpacingAroundKeywordRuleTest {
    private val spacingAroundKeywordRuleAssertThat = SpacingAroundKeywordRule().assertThat()

    @Test
    fun `Given an if-statement without space after the if`() {
        val code =
            """
            fun main() {
                if(true) {}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                if (true) {}
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 7, "Missing spacing after \"if\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-else-statement without space after the else`() {
        val code =
            """
            fun main() {
                if (true) {} else{}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                if (true) {} else {}
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 22, "Missing spacing after \"else\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-else-statement with an unexpected newline before the else`() {
        val code =
            """
            fun main() {
                if (true) {}
                else {}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                if (true) {} else {}
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(3, 5, "Unexpected newline before \"else\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a while-statement without space after the while`() {
        val code =
            """
            fun main() {
                while(true) {}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                while (true) {}
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 10, "Missing spacing after \"while\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a do-while-statement without space after the do`() {
        val code =
            """
            fun main() {
                do{} while (true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                do {} while (true)
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 7, "Missing spacing after \"do\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a do-while-statement without space after the while`() {
        val code =
            """
            fun main() {
                do {} while(true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                do {} while (true)
            }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 16, "Missing spacing after \"while\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-statement without space after the try`() {
        val code =
            """
            val foo = try{ "".trim() } catch (e: Exception) {}
            """.trimIndent()
        val formattedCode =
            """
            val foo = try { "".trim() } catch (e: Exception) {}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(1, 14, "Missing spacing after \"try\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-statement without space after the catch`() {
        val code =
            """
            val foo = try { "".trim() } catch(e: Exception) {}
            """.trimIndent()
        val formattedCode =
            """
            val foo = try { "".trim() } catch (e: Exception) {}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(1, 34, "Missing spacing after \"catch\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-finally-statement without space after the catch`() {
        val code =
            """
            val foo = try { "".trim() } catch (e: Exception) {} finally{}
            """.trimIndent()
        val formattedCode =
            """
            val foo = try { "".trim() } catch (e: Exception) {} finally {}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(1, 60, "Missing spacing after \"finally\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-statement with an unexpected newline before the catch`() {
        val code =
            """
            val foo = try { "".trim() }
                catch (e: Exception) {}
            """.trimIndent()
        val formattedCode =
            """
            val foo = try { "".trim() } catch (e: Exception) {}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 5, "Unexpected newline before \"catch\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a try-catch-finally statement with an unexpected newline before the finally`() {
        val code =
            """
            val foo = try { "".trim() } catch (e: Exception) {}
                finally {}
            """.trimIndent()
        val formattedCode =
            """
            val foo = try { "".trim() } catch (e: Exception) {} finally {}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 5, "Unexpected newline before \"finally\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a getter with an unexpected space after the get`() {
        val code =
            """
            var x: String
                get () {
                    return ""
                }
            """.trimIndent()
        val formattedCode =
            """
            var x: String
                get() {
                    return ""
                }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 5, "Unexpected spacing after \"get\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a setter with an unexpected space after the set`() {
        val code =
            """
            var x: String
                private set (value) {
                    x = value
                }
            """.trimIndent()
        val formattedCode =
            """
            var x: String
                private set(value) {
                    x = value
                }
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(2, 13, "Unexpected spacing after \"set\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a setter with an unexpected newline after the set`() {
        val code =
            """
            var setterVisibility: String = "abc"
                private set
            var setterWithAnnotation: Any? = null
                @Inject set
            var setterOnNextLine: String
                private set
                (value) { setterOnNextLine = value}
            """.trimIndent()
        val formattedCode =
            """
            var setterVisibility: String = "abc"
                private set
            var setterWithAnnotation: Any? = null
                @Inject set
            var setterOnNextLine: String
                private set(value) { setterOnNextLine = value}
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasLintViolation(6, 13, "Unexpected spacing after \"set\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some KDoc containing keywords`() {
        val code =
            """
            /**
             * Convenience wrapper around [Mockito.when] to avoid special `when` notation.
             */
            fun <T> whenever(call: T): OngoingStubbing<T> = Mockito.`when`(call)
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an if-else-statement where the true branch contains a when statement which is not wrapped in a block then the else should not be forced after the closing brace of the when`() {
        val code =
            """
            val foo =
                if (true)
                    when {
                        else -> 1
                    }
                else 2
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an if-else-statement where the true branch contains a try-catch-statement which is not wrapped in a block then the else should not be forced after the closing brace of the try-catch`() {
        val code =
            """
            val foo =
                if (true)
                    try { 1 } catch (e: Exception) { 2 }
                else 3
            """.trimIndent()
        spacingAroundKeywordRuleAssertThat(code)
            .hasNoLintViolations()
    }
}
