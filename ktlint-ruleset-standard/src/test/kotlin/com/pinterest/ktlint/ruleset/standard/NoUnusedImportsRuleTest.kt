package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoUnusedImportsRuleTest {
    private val noUnusedImportsRuleAssertThat = NoUnusedImportsRule().assertThat()

    @Disabled("To be fixed")
    @Test
    fun `Given that the first import is unused than the file should not start with a linebreak`() {
        val code =
            """
            import foo.unused
            import foo.used

            fun main() {
                used()
            }
            """.trimIndent()
        val formattedCode =
            """
            import foo.used

            fun main() {
                used()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unused import")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that function with name 'a' is called then do not remove imports ending with 'a' as path element`() {
        val code =
            """
            import foo.a
            import foo.bar.a

            fun main() {
                println(a())
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that an instance of class B is created then do not remove imports ending with 'B' as path element`() {
        val code =
            """
            import foo.B
            import foo.bar.B

            fun main() {
                B()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that function with name 'a' is called statically on class 'C' then do not remove imports ending with 'C' as path element`() {
        val code =
            """
            import foo.C
            import foo.bar.C

            fun main() {
                C.a()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that an infix function with name 'someInfixFunction' is called then do not remove imports ending with 'someInfixFunction' as path element`() {
        val code =
            """
            import foo.someInfixFunction
            import foo.bar.someInfixFunction

            fun main() {
                1 someInfixFunction 2
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that a function with name 'foo Bar' between backticks is called then do not remove imports ending with 'foo Bar' as path element `() {
        val code =
            """
            import foo.`foo Bar`
            import foo.bar.`foo Bar`

            fun main() {
                `foo Bar`()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that a function with name 'fooBar' without backticks is called then do not remove imports ending with 'fooBar' or 'fooBar' between backticks as path element `() {
        val code =
            """
            import foo.`fooBar` // Backticks are redundant but are not disallowed
            import foo.bar.`fooBar` // Backticks are redundant but are not disallowed
            import foo2.fooBar
            import foo2.bar.fooBar

            fun main() {
                fooBar()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that a function with name 'fooBar' between redundant backticks is called then do not remove imports ending with 'fooBar' as path element `() {
        val code =
            """
            import foo.`fooBar` // Backticks are redundant but are not disallowed
            import foo.bar.`fooBar` // Backticks are redundant but are not disallowed
            import foo2.fooBar
            import foo2.bar.fooBar

            fun main() {
                `fooBar`() // Backticks are redundant but are not disallowed
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given import plusAssign and usage of += operator then do not return a lint error`() {
        val code =
            """
            import rx.lang.kotlin.plusAssign

            fun main() {
                v += 1
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a declaration with value 'anotherThing' then do not remove imports ending with 'anotherThing' as path element`() {
        val code =
            """
            import com.example.anotherThing

            class Foo {
                val bar = anotherThing
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class DestructuringDeclaration {
        @Test
        fun `Given a destructuring declaration then do not remove imports ending with 'componentN' where 'N' is any number`() {
            val code =
                """
                import foo.component6
                import foo.component1234
                import foo.bar.component6
                import foo.bar.component789

                fun main() {
                    val (one, two, three, four, five, six) = someList
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a destructuring declaration then do remove imports ending with 'componentN' where 'N' is not a number`() {
            val code =
                """
                import p.component6
                import p.component
                import p.component12woohoo

                fun main() {
                    val (one, two, three, four, five, six) = someList
                }
                """.trimIndent()
            val formattedCode =
                """
                import p.component6

                fun main() {
                    val (one, two, three, four, five, six) = someList
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unused import"),
                    LintViolation(3, 1, "Unused import")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun testLintKDocLinkImport() {
        val code =
            """
            package kdoc

            import DRef
            import p.PDRef
            import DRef2
            import p.PDRef2
            import p.DRef3
            import p.PDRef3
            import p.PDRef4
            import p.PDRef5
            import p.O

            /**
             * [DRef] DRef2
             * [O.method]
             * [p.PDRef] p.PDRef2
             * [PDRef3](p.DRef3) p.PDRef4 PDRef5 Note that [PDRef3](p.DRef3) is not recognized as markdown link because of "](" and
             *    of such "p.DRef3" is marked as unused import
             * [] text
             */
            fun main() {}
            """.trimIndent()
        val formattedCode =
            """
            package kdoc

            import DRef
            import p.PDRef
            import p.O

            /**
             * [DRef] DRef2
             * [O.method]
             * [p.PDRef] p.PDRef2
             * [PDRef3](p.DRef3) p.PDRef4 PDRef5 Note that [PDRef3](p.DRef3) is not recognized as markdown link because of "](" and
             *    of such "p.DRef3" is marked as unused import
             * [] text
             */
            fun main() {}
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 1, "Unused import"),
                LintViolation(6, 1, "Unused import"),
                LintViolation(7, 1, "Unused import"),
                LintViolation(8, 1, "Unused import"),
                LintViolation(9, 1, "Unused import"),
                LintViolation(10, 1, "Unused import")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some unnecessary imports`() {
        val code =
            """
            import C0 as C0X
            import C1
            import C1 as C1X
            import `C2`
            import `C2` as C2X
            import C3.method

            fun main() {
                println(C0X, C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        val formattedCode =
            """
            import C0 as C0X
            import C1 as C1X
            import `C2` as C2X
            import C3.method

            fun main() {
                println(C0X, C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unnecessary import"),
                LintViolation(4, 1, "Unnecessary import")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some unused imports from the same package`() {
        val code =
            """
            package p

            import p.C1
            import p.C1 as C1X
            import p.`C2`
            import p.`C2` as C2X
            import p.C3.method

            fun main() {
                println(C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        val formattedCode =
            """
            package p

            import p.C1 as C1X
            import p.`C2` as C2X
            import p.C3.method

            fun main() {
                println(C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unnecessary import"),
                LintViolation(5, 1, "Unnecessary import")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some duplicated imports then remove the duplicates`() {
        val code =
            """
            import org.repository.RepositoryPolicy
            import org.repository.any
            import org.repository.all
            import org.repository.any
            import org.repository.any
            import org.repository.all
            fun main() {
                    RepositoryPolicy(
                    any(false), all("trial")
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            import org.repository.RepositoryPolicy
            import org.repository.any
            import org.repository.all
            fun main() {
                    RepositoryPolicy(
                    any(false), all("trial")
                )
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 1, "Unused import"),
                LintViolation(5, 1, "Unused import"),
                LintViolation(6, 1, "Unused import")
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class ParentImport {
        @Test
        fun `Given that no package statement is present and an import is used for which the parent is also imported then remove the direct import`() {
            val code =
                """
                import org.mockito.Mockito
                import org.mockito.Mockito.withSettings
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            val formattedCode =
                """
                import org.mockito.Mockito
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unused import")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given that a package statement is present and an import is used for which the parent is also imported then remove the direct import`() {
            val code =
                """
                package com.example

                import org.mockito.Mockito
                import org.mockito.Mockito.withSettings
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            val formattedCode =
                """
                package com.example

                import org.mockito.Mockito
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolation(4, 1, "Unused import")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an import of a static variable for which also the parent imported then do not remove the direct import`() {
            val code =
                """
                import org.repository.RepositoryPolicy
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                fun main() {
                        RepositoryPolicy(
                        false, "trial",
                        CHECKSUM_POLICY_IGNORE
                    )
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given an import of a static variable for which also the parent imported but not used then do remove the parent import`() {
            val code =
                """
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                import org.repository.RepositoryPolicy
                fun main() {
                       val a = CHECKSUM_POLICY_IGNORE
                }
                """.trimIndent()
            val formattedCode =
                """
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                fun main() {
                       val a = CHECKSUM_POLICY_IGNORE
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unused import")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun testFormatWhenParentImportWithStaticVariableAndMethod() {
            val code =
                """
                package org.tw.project
                import org.repository.RepositoryPolicy
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                import org.mockito.Mockito
                import org.mockito.Mockito.withSettings
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                }
                fun main() {
                       val a = CHECKSUM_POLICY_IGNORE
                }
                """.trimIndent()
            val formattedCode =
                """
                package org.tw.project
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                import org.mockito.Mockito
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                }
                fun main() {
                       val a = CHECKSUM_POLICY_IGNORE
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unused import"),
                    LintViolation(5, 1, "Unused import")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class ProvideDelegate {
        @Test
        fun `Issue 513 - provideDelegate is allowed if there is a by keyword`() {
            val code =
                """
                import org.gradle.api.Plugin
                import org.gradle.api.Project
                import org.gradle.api.tasks.WriteProperties
                import org.gradle.kotlin.dsl.getValue
                import org.gradle.kotlin.dsl.provideDelegate
                import org.gradle.kotlin.dsl.registering

                class DumpVersionProperties : Plugin<Project> {
                    override fun apply(target: Project) {
                        with(target) {
                            val dumpVersionProperties by tasks.registering(WriteProperties::class) {
                                setProperties(mapOf("version" to "1.2.3"))
                                outputFile = rootDir.resolve("version.properties")
                            }

                        }
                    }
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `provideDelegate is not allowed without by keyword`() {
            val code =
                """
                import org.gradle.kotlin.dsl.provideDelegate
                import com.github.ajalt.clikt.parameters.groups.provideDelegate

                fun main() {
                }
                """.trimIndent()
            val formattedCode =
                // TODO: replace trimMargin with trimImdent when bug with removal of first import is resolved
                """
                |
                |
                |fun main() {
                |}
                """.trimMargin()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 1, "Unused import"),
                    LintViolation(2, 1, "Unused import")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 669 - provideDelegate is allowed for any import path`() {
            val code =
                """
                import com.github.ajalt.clikt.parameters.groups.provideDelegate

                fun main() {
                    private val old by argument("OLD", help = "Old input file.")
                      .path(exists = true, folderOkay = false, readable = true, fileSystem = inputFs)
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun shouldNotReportUnusedWhenStaticImportIsFromAnotherPackage() {
        val code =
            """
            package com.foo

            import android.text.Spannable
            import androidx.core.text.toSpannable

            fun foo(text: String): Spannable {
                return text.toSpannable()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `should import alias after as and not when as is present in package name`() {
        val code =
            """
            package com.fastcompany.foo

            import com.fastcompany.common.Awesome as CommonAsset
            import com.company.common.alias as CommonAsset

            class SomeConfig {
                fun fooFunction() = CommonAsset()
            }

            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `should import alias after as and not when as is present in package ending name`() {
        val code =
            """
            import com.company.common.alias as CommonAsset

            class SomeConfig {

                fun fooFunction() = CommonAsset()
            }

            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `if import path contains 'import' word - does not report issues`() {
        val code =
            """
            import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
            import com.pinterest.ktlint.ruleset.standard.internal.importordering.parseImportsLayout
            import org.assertj.core.api.Assertions.assertThat
            import org.junit.jupiter.api.Test

            class ImportLayoutParserTest {
                @Test(expected = IllegalArgumentException::class)
                fun `blank lines in the beginning and end of import list are not allowed`() {
                    parseImportsLayout("|,*,|")
                }

                @Test(expected = IllegalArgumentException::class)
                fun `pattern without single wildcard is not allowed`() {
                    parseImportsLayout("java.util.List")
                }

                @Test
                fun `parses correctly`() {
                    val expected = listOf(
                        PatternEntry("android", withSubpackages = true, hasAlias = false),
                        PatternEntry.BLANK_LINE_ENTRY,
                        PatternEntry("org.junit", withSubpackages = true, hasAlias = false),
                        PatternEntry.BLANK_LINE_ENTRY,
                        PatternEntry("android", withSubpackages = true, hasAlias = true),
                        PatternEntry.ALL_OTHER_IMPORTS_ENTRY,
                        PatternEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
                    )
                    val actual = parseImportsLayout("android.*,|,org.junit.*,|,^android.*,*,^*")

                    assertThat(actual).isEqualTo(expected)
                }
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `only redundant static imports should be removed`() {
        val code =
            """
            import com.foo.psi.abc
            import com.foo.psi.findAnnotation

            fun main() {
                val psi = getPsi()
                psi.abc()
                val bar = psi.findAnnotation(SOME_CONSTANT)
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `only redundant sealed sub class imports should be removed`() {
        val code =
            """
            import com.foo.psi.Sealed
            import com.foo.psi.Sealed.SubClass

            fun main() {
                listOf<Sealed>()
                Sealed.SubClass()
                SubClass()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `only redundant sealed sub class imports should be removed 2`() {
        val code =
            """
            import com.zak.result.Result.Expected
            import com.zak.result.Result.Unexpected
            import org.assertj.core.api.Assertions.assertThat

            fun test() {
                assertThat(Result.just(1)).isEqualTo(Expected(1))
                assertThat(Result.just(1)).isEqualTo(Result.Expected(1))

                val ex = Exception()
                assertThat(Result.raise(exception)).isEqualTo(Unexpected(ex))
                assertThat(Result.raise(exception)).isEqualTo(Result.Unexpected(exception))
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `only redundant static java function imports should be removed`() {
        val code =
            """
            import com.google.cloud.bigtable.data.v2.models.Mutation
            import com.google.cloud.bigtable.data.v2.models.Row
            import com.google.cloud.bigtable.data.v2.models.RowMutation.create

            fun test(row: Row) {
                create("string", "string", Mutation.create())
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `import directive has backticks and alias`() {
        val code =
            """
            import com.test.`object`.Producer as Producer1

            class Consumer(producer1: Producer1<String>) {
                val x = 1
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `repeated invocations on the same rule instance should work`() {
        val codeFile1 =
            """
            import foo.bar

            fun main() {
                bar()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(codeFile1).hasNoLintViolations()

        // Reuse the noUnusedImportsRuleAssertThat (e.g. reuse the UnusedImportsRule instance) for the invocation on the
        // second code sample
        val codeFile2 =
            """
            import foo.bar
            import foo.baz

            fun main() {
                bar()
                baz()
            }
            """.trimIndent()
        noUnusedImportsRuleAssertThat(codeFile2).hasNoLintViolations()
    }

    @Nested
    inner class WildcardImports {
        // Solution for #1256 has been reverted as it can lead to removal of imports which are actually used (see test for
        // #1277). For now, there seems to be no reliable way to determine whether the wildcard import is actually used or
        // not.
        @Disabled
        @Test
        fun `Issue 1256 - remove wildcard import when not used`() {
            val code =
                """
                import abc.*
                import def.*
                import def.Something

                fun foo() = def.Something()
                """.trimIndent()
            val formattedCode =
                """
                import def.*
                import def.Something

                fun foo() = def.Something()
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 1, "Unnecessary import"),
                    LintViolation(2, 1, "Unnecessary import")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 1277 - Wildcard import should not be removed because it can not be reliable be determined whether it is used`() {
            val code =
                """
                import test.*

                fun main() {
                    Test() // defined in package test
                }
                """.trimIndent()
            noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Issue 1243 - Given imports for the same class but with different aliases then the imports should not be removed when used`() {
        val code =
            """
            import foo.Bar as Bar1
            import foo.Bar as Bar2

            val bar1 = Bar1()
            val bar2 = Bar2()
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1282 - do not remove import when used in kdoc only`() {
        val code =
            """
            import some.pkg.returnSelf

            /**
             * Do not forget that you can also return string via [String.returnSelf]
             */
            fun test() {}
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1393 - Wildcard import should not be removed because it can not be reliable be determined whether it is used`() {
        val code =
            """
            package com.example

            import com.example.Outer.*

            class Outer {
                class Inner
            }

            val foo = Inner()
            """.trimIndent()
        noUnusedImportsRuleAssertThat(code).hasNoLintViolations()
    }
}
