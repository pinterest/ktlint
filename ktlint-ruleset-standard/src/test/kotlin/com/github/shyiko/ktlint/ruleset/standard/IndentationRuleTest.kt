package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class IndentationRuleTest {

    @Test
    fun testLint() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
                val a = 0
                    val b = 0
                if (a == 0) {
                    println(a)
                }
                val b = builder().setX().setY()
                    .build()
               val c = builder("long_string" +
                     "")
            }

            class A {
                var x: String
                    get() = ""
                    set(v: String) { x = v }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(12, 1, "indent", "Unexpected indentation (3) (it should be 4)"),
            // fixme: expected indent should not depend on the "previous" line value
            LintError(13, 1, "indent", "Unexpected indentation (9) (it should be 7)")
        ))
    }

    @Test
    fun testLintNested() {
        assertThat(IndentationRule().lint(
            """
            fun funA() =
                  doStuff().use {
                      while (it.moveToNext()) {
                          doMore()
                      }
                  }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintCustomIndentSize() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "3")
        )).isEqualTo(listOf(
            LintError(3, 1, "indent", "Unexpected indentation (4) (it should be 3)")
        ))
    }

    @Test
    fun testLintCustomIndentSizeValid() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
              val v = ""
              println(v)
            }

            class A {
              var x: String
                get() = ""
                set(v: String) { x = v }
            }
            """.trimIndent(),
            mapOf("indent_size" to "2")
        )).isEmpty()
    }

    @Test
    fun testLintIndentSizeUnset() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "unset")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndent() {
        assertThat(IndentationRule().lint(
            """
                class TestContinuation {
                    fun main() {
                        val list = listOf(
                              listOf(
                                    "string",
                                    "another string"
                              ),
                              listOf("one", "two")
                        )
                    }
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentConcatenation() {
        assertThat(IndentationRule().lint(
            """
                class TestSubClass {
                    fun asdf(string: String) = string
                    val c = asdf("long_string" +
                          "")
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentDotQualifiedExpression() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    ClassA()
                          .methodA()
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintIndentInsideObjectBody() {
        assertThat(IndentationRule().lint(
            """
                @Test fun field() {
                    field.validateWith()
                          .handleWith(object : InterfaceA {
                              override fun handleFailed(input: String, errors: List<String>) {
                                  failedMessages.addAll(errors)
                              }

                              override fun handleSucceeded() {
                                  succeededMessages.add("success")
                              }
                          })
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    // fixme: should each argument be on a separate line?
    @Test
    fun testLintContinuationIndentFunctionCallArgumentList() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    val valueA =
                          listOf(ClassA(),
                                ClassB())
                }
                fun data() = listOf(
                      with(ClassA()) {
                          arrayOf({ paramA: TypeA ->
                              paramA.build()
                          }, funB())
                      },
                      arrayOf({ paramA: TypeA -> paramA.build() },
                            funB()
                      )
                )
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentInsideParenthesis() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    val valA = ClassA(
                          field = (
                                ClassB(
                                      fieldBOne = "one",
                                      fieldBTwo = "two",
                                      fieldBThree = 0
                                ))
                    )
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintIndentGettersAndSetters() {
        assertThat(IndentationRule().lint(
            """
                val storyBody: String
                    get() = String.format(body, "")
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentSuperTypeList() {
        assertThat(IndentationRule().lint(
            """
            class ClassA(fieldA: TypeA, fieldB: TypeB = DefaultB) :
                  SuperClassA(fieldA, fieldB)
            class ClassA(a: TypeA) :
                  BasePresenter<View>() {

                private lateinit var view: View
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintIndentInsideFunctionBody() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    val valueA = ClassA()
                    valueA.doStuff()
                    assertThat(valueA.getFieldB()).isEqualTo(100L)
                }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentInsideSuperTypeList() {
        assertThat(IndentationRule().lint(
            """
            class ClassA : ClassB(), InterfaceA,
                  InterfaceB {
            }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentTypeProjection() {
        assertThat(IndentationRule().lint(
            """
            val variable: SuperTpe<TypeA,
                  TypeB> = Implementation()
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentAssignment() {
        assertThat(IndentationRule().lint(
            """
            fun funA() {
                val (a, b, c) =
                      anotherFun()
            }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test()
    fun testLintContinuationIndentTypeCasting() {
        assertThat(IndentationRule().lint(
            """
            fun funA() = funB() as
                  TypeA
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentConstructorDelegation() {
        assertThat(IndentationRule().lint(
            """
            class A : B() {
                constructor(a: String) :
                      this(a)
            }
            class MyClass{
                constructor(a: TypeA) :
                      super(a) {
                    init(a)
                }
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentSafeChaining() {
        assertThat(IndentationRule().lint(
            """
            val valueA = call()
                  //comment
                  ?.chainCallC { it.anotherCall() }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintContinuationIndentTypeDeclaration() {
        assertThat(IndentationRule().lint(
            """
            private fun funA(a: Int, b: String):
                  MyTypeA {
                return MyTypeA(a, b)
            }
              """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testLintCommentsAreIgnored() {
        assertThat(IndentationRule().lint(
            """
            fun funA(argA: String) =
                  // comment
            // comment
                  call(argA)
            fun main() {
                addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
             // comment
                    override fun onLayoutChange(
                    )
                })
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEqualTo(listOf(
            LintError(7, 1, "indent", "Unexpected indentation (1) (it should be 8)")
        ))
    }
}
