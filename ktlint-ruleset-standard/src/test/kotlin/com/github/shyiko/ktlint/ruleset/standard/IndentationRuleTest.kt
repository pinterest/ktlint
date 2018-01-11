package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class IndentationRuleTest {

    @Test
    fun testRule() {
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
            LintError(13, 1, "indent", "Unexpected indentation (5) (it should be 4)")
        ))
    }

    @Test
    fun testVerticallyAlignedParametersDoNotTriggerAnError() {
        assertThat(IndentationRule().lint(
            """
            data class D(val a: Any,
                         @Test val b: Any,
                         val c: Any = 0) {
            }

            data class D2(
                val a: Any,
                val b: Any,
                val c: Any
            ) {
            }

            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }

            fun f2(
                a: Any,
                b: Any,
                c: Any
            ) {
            }
            """.trimIndent()
        )).isEmpty()
        assertThat(IndentationRule().lint(
            """
            class A(
               //
            ) {}
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 1, "indent", "Unexpected indentation (3) (it should be 4)")
        ))
    }

    @Test
    fun testWithCustomIndentSize() {
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
    fun testErrorWithCustomIndentSize() {
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
    fun testErrorWithIndentSizeUnset() {
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
    fun testShouldReportIncorrectIndentOfFirstParameter() {
        assertThat(IndentationRule().lint(
            """
            fun x(
                 x: Int = 0,
                y: Int = 0
            ) {
            }
            """.trimIndent(),
            script = true
        )).isEqualTo(listOf(
            LintError(2, 1, "indent", "Unexpected indentation (5) (it should be 4)")
        ))
    }

    @Test
    fun testShouldRespectContinuationIndent() {
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
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun `testUseContinuationIndentForConcatenation`() {
        assertThat(IndentationRule().lint(
            """
                class TestSubClass {
                    fun asdf(string: String) = string
                    val c = asdf("long_string" +
                          "")
                }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForDotQualifiedExpression() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    ClassA()
                          .methodA()
                }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseIndentForObjectImplementation() {
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
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testComplexAssignmentQualifiedAccessAndFunctionBody() {
        assertThat(IndentationRule().lint(
            """
            fun funA() =
                  doStuff().use {
                      while (it.moveToNext()) {
                          doMore()
                      }
                  }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForArgumentList() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    val valueA =
                          listOf(ClassA(),
                                ClassB())
                }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentInsideParenthesis() {
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
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseIndentForCustomGetter() {
        assertThat(IndentationRule().lint(
            """
                val storyBody: String
                    get() = String.format(body, "")
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentAfterAssignment() {
        assertThat(IndentationRule().lint(
            """
        val valueA =
              "{\"title\"}"
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForSuperTypeList() {
        assertThat(IndentationRule().lint(
            """
            class ClassA(fieldA: TypeA,
                         fieldB: TypeB = DefaultB) :
                  SuperClassA(fieldA, fieldB)
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseIndentForFunctionBody() {
        assertThat(IndentationRule().lint(
            """
                fun funA() {
                    val valueA = ClassA()
                    valueA.doStuff()
                    assertThat(valueA.getFieldB()).isEqualTo(100L)
                }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentInsideSuperTypeList() {
        assertThat(IndentationRule().lint(
            """
            class ClassA : ClassB(), InterfaceA,
                  InterfaceB {
            }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForTypeProjection() {
        assertThat(IndentationRule().lint(
            """
            val variable: SuperTpe<TypeA,
                  TypeB> = Implementation()
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test(enabled = false)
        //not sure if it should use continuation indent or same as parameters
    fun testCommentBetweenParameterListShouldUseSameIndent() {
        assertThat(IndentationRule().lint(
            """
            data class MyClass(val a: String,
                               val b: String,
                  //comment between properties
                               val c: String)
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForAssignment() {
        assertThat(IndentationRule().lint(
            """
            fun funA() {
                val (a, b, c) =
                      anotherFun()
            }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test()
    fun testUseContinuationIndentForTypeCasting() {
        assertThat(IndentationRule().lint(
            """
            fun funA() = funB() as
                  TypeA
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForConstructorDelegation() {
        assertThat(IndentationRule().lint(
            """
            class A : B() {
                constructor(a: String) :
                      this(a)
            }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun shouldUseContinuationInsideSafeQualifiedExpression() {
        assertThat(IndentationRule().lint(
            """
            val valueA = call()
                  //comment
                  ?.chainCallC { it.anotherCall() }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testUseContinuationIndentForTypeDeclaration() {
        assertThat(IndentationRule().lint(
            """
            private fun funA(a: Int, b: String):
                  MyTypeA {
                return MyTypeA(a, b)
            }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testIgnoreSuperTypeListWhenCalculatePreviousIndent() {
        assertThat(IndentationRule().lint(
            """
            class ClassA(a: TypeA) :
                  BasePresenter<View>() {

                private lateinit var view: View
            }
              """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testIgnoreConstructorDelegationCallWhenCalculatingPreviousIntent() {
        assertThat(IndentationRule().lint(
            """
                class MyClass{
                    constructor(a: TypeA) :
                          super(a) {
                        init(a)
                    }
                }
            """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test(enabled = false)
        //Not sure it should be supported. Recommended way can be to put each argument on separate line
    fun testFuncIndent() {
        assertThat(IndentationRule().lint(
            """
            fun funA(a: A, b: B) {
                return funB(a,
                      b, { (id) ->
                    funC(id)
                }
                )
            }
            """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testComplexValueArgumentUsage() {
        assertThat(IndentationRule().lint(
            """
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
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }

    @Test
    fun testIgnoreCommentWhenCalculateParentIndent() {
        assertThat(IndentationRule().lint(
            """
            fun funA(argA: String) =
                  // comment
                  // comment
                  call(argA)
            """.trimIndent(),
            mapOf("indent_size" to "4",
                "continuation_indent_size" to "6")
        )).isEmpty()
    }
}
