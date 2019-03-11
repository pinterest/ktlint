package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class AnnotationRuleTest {

    @Test
    fun `lint single annotation may be placed on line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @FunctionalInterface class A {
                    @JvmField
                    var x: String
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format single annotation may be placed on line before annotated construct`() {
        val code =
            """
            @FunctionalInterface class A {
                @JvmField
                var x: String
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint single annotation may be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @FunctionalInterface class A {
                    @JvmField var x: String

                    @Test fun myTest() {}
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format single annotation may be placed on same line as annotated construct`() {
        val code =
            """
            @FunctionalInterface class A {
                @JvmField var x: String

                @Test fun myTest() {}
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint multiple annotations should not be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmField @Volatile var x: String

                    @JvmField @Volatile
                    var y: String
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2, 5, "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            )
        )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                class A {
                    @JvmField @Volatile var x: String

                    @JvmField @Volatile
                    var y: String
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                @JvmField @Volatile
                var x: String

                @JvmField @Volatile
                var y: String
            }
            """.trimIndent()
        )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct (with no previous whitespace)`() {
        assertThat(AnnotationRule().format("@JvmField @Volatile var x: String"))
            .isEqualTo(
                """
                @JvmField @Volatile
                var x: String
                """.trimIndent()
            )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct (with no previous indent)`() {
        assertThat(
            AnnotationRule().format(
                """

                @JvmField @Volatile var x: String
                """.trimIndent()
            )
        ).isEqualTo(
            """

            @JvmField @Volatile
            var x: String
            """.trimIndent()
        )
    }

    @Test
    fun `lint annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmName("xJava") var x: String

                    @JvmName("yJava")
                    var y: String
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2, 5, "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                class A {
                    @JvmName("xJava") var x: String

                    @JvmName("yJava")
                    var y: String
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                @JvmName("xJava")
                var x: String

                @JvmName("yJava")
                var y: String
            }
            """.trimIndent()
        )
    }

    @Test
    fun `lint multiple annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @Retention(SOURCE) @Target(FUNCTION, PROPERTY_SETTER, FIELD) annotation class A

                @Retention(SOURCE)
                @Target(FUNCTION, PROPERTY_SETTER, FIELD)
                annotation class B
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                1, 1, "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            ),
            LintError(
                1, 1, "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format multiple annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                @Retention(SOURCE) @Target(FUNCTION, PROPERTY_SETTER, FIELD) annotation class A

                @Retention(SOURCE)
                @Target(FUNCTION, PROPERTY_SETTER, FIELD)
                annotation class B
                """.trimIndent()
            )
        ).isEqualTo(
            """
            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY_SETTER, FIELD)
            annotation class A

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY_SETTER, FIELD)
            annotation class B
            """.trimIndent()
        )
    }

    @Test
    fun `lint annotation after keyword`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    private @Test fun myTest() {}
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format annotation after keyword`() {
        val code =
            """
            class A {
                private @Test fun myTest() {}
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint multi-line annotation`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmField @Volatile @Annotation(
                        enabled = true,
                        groups = [
                            "a",
                            "b",
                            "c"
                        ]
                    ) val a: Any
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2, 5, "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            ),
            LintError(
                2, 5, "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format multi-line annotation`() {
        val code =
            """
            class A {
                @JvmField @Volatile @Annotation(
                    enabled = true,
                    groups = [
                        "a",
                        "b",
                        "c"
                    ]
                ) val a: Any
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(
            """
            class A {
                @JvmField
                @Volatile
                @Annotation(
                    enabled = true,
                    groups = [
                        "a",
                        "b",
                        "c"
                    ]
                )
                val a: Any
            }
            """.trimIndent()
        )
    }
}
