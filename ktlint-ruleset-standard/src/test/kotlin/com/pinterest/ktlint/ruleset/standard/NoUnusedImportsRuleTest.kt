package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoUnusedImportsRuleTest {
    @Test
    fun testLint() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import p.a
                import p.B6
                import java.nio.file.Paths
                import p.B as B12
                import p2.B
                import p.C
                import p.a.*
                import escaped.`when`
                import escaped.`foo`
                import p.infixfunc

                fun main() {
                    println(a())
                    C.call(B())
                    1 infixfunc 2
                    `when`()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "no-unused-imports", "Unused import"),
                LintError(3, 1, "no-unused-imports", "Unused import"),
                LintError(4, 1, "no-unused-imports", "Unused import"),
                LintError(9, 1, "no-unused-imports", "Unused import")
            )
        )
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import rx.lang.kotlin.plusAssign

                fun main() {
                    v += 1
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintIssue204() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                package com.example.another

                import com.example.anotherThing

                class Foo {
                    val bar = anotherThing
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintDestructuringAssignment() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import p.component6

                fun main() {
                    val (one, two, three, four, five, six) = someList
                }
                """.trimIndent()
            )
        ).isEmpty()
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import p.component6
                import p.component2
                import p.component100
                import p.component
                import p.component12woohoo

                fun main() {
                    val (one, two, three, four, five, six) = someList
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(4, 1, "no-unused-imports", "Unused import"),
                LintError(5, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    @Test
    fun testLintKDocLinkImport() {
        assertThat(
            NoUnusedImportsRule().lint(
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
                 * [PDRef3](p.DRef3) p.PDRef4 PDRef5
                 * [] text
                 */
                fun main() {}
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(4, 1, "no-unused-imports", "Unused import"),
                LintError(5, 1, "no-unused-imports", "Unused import"),
                LintError(6, 1, "no-unused-imports", "Unused import"),
                LintError(7, 1, "no-unused-imports", "Unused import"),
                LintError(8, 1, "no-unused-imports", "Unused import"),
                LintError(9, 1, "no-unused-imports", "Unused import"),
                LintError(10, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    @Test
    fun testSamePackageImport() {
        assertThat(
            NoUnusedImportsRule().lint(
                """


                import C1
                import C1 as C1X
                import `C2`
                import `C2` as C2X
                import C3.method

                fun main() {
                    println(C1, C1X, C2, C2X, method)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(3, 1, "no-unused-imports", "Unnecessary import"),
                LintError(5, 1, "no-unused-imports", "Unnecessary import")
            )
        )
        assertThat(
            NoUnusedImportsRule().lint(
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
            )
        ).isEqualTo(
            listOf(
                LintError(3, 1, "no-unused-imports", "Unnecessary import"),
                LintError(5, 1, "no-unused-imports", "Unnecessary import")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            NoUnusedImportsRule().format(
                """
                import p.a
                import p.B6
                import p.B as B12
                import p2.B as B2
                import p.C
                import escaped.`when`
                import escaped.`foo`

                fun main() {
                    println(a())
                    C.call()
                    fn(B2.NAME)
                    `when`()
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            import p.a
            import p2.B as B2
            import p.C
            import escaped.`when`

            fun main() {
                println(a())
                C.call()
                fn(B2.NAME)
                `when`()
            }
            """.trimIndent()
        )
    }

    @Test
    fun testFormatKDocLinkImport() {
        assertThat(
            NoUnusedImportsRule().format(
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

                /**
                 * [DRef] DRef2
                 * [p.PDRef] p.PDRef2
                 * [PDRef3](p.DRef3) p.PDRef4 PDRef5
                 */
                fun main() {}
                """.trimIndent()
            )
        ).isEqualTo(
            """
            package kdoc

            import DRef

            /**
             * [DRef] DRef2
             * [p.PDRef] p.PDRef2
             * [PDRef3](p.DRef3) p.PDRef4 PDRef5
             */
            fun main() {}
            """.trimIndent()
        )
    }

    @Test
    fun testParentPackImport() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import org.mockito.Mockito
                import org.mockito.Mockito.withSettings
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    @Test
    fun testParentPackImportWithPackageNamePresent() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                package org.tw.project
                import org.mockito.Mockito
                import org.mockito.Mockito.withSettings
                fun foo() {
                        Mockito.mock(String::class.java, Mockito.withSettings().defaultAnswer {  })
                    }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(3, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    @Test
    fun testParentPackImportWithImportNameHavingNoParentImport() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import org.assertj.core.util.diff.DiffUtils.diff
                import org.assertj.core.util.diff.DiffUtils.generateUnifiedDiff
                fun foo() {
                val a = diff(2)
                val diff = generateUnifiedDiff(1)
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testParentImportWithStaticVariable() {
        assertThat(
            NoUnusedImportsRule().lint(
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
            )
        ).isEmpty()
    }

    @Test
    fun testParentImportWithStaticVariableImportAndParentImportUnused() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import org.repository.RepositoryPolicy
                import org.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
                fun main() {
                       val a = CHECKSUM_POLICY_IGNORE
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    @Test
    fun testFormatWhenParentImportWithStaticVariableAndMethod() {
        assertThat(
            NoUnusedImportsRule().format(
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
            )
        ).isEqualTo(
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
        )
    }

    @Test
    fun `provideDelegate is allowed if there is a by keyword`() {
        assertThat(
            NoUnusedImportsRule().lint(
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
            )
        ).isEmpty()
    }

    @Test
    fun `provideDelegate is not allowed without by keyword`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import org.gradle.kotlin.dsl.provideDelegate
                import com.github.ajalt.clikt.parameters.groups.provideDelegate

                fun main() {
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-unused-imports", "Unused import"),
                LintError(2, 1, "no-unused-imports", "Unused import")
            )
        )
    }

    // https://github.com/pinterest/ktlint/issues/669
    @Test
    fun `provideDelegate is allowed for any import path`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.github.ajalt.clikt.parameters.groups.provideDelegate

                fun main() {
                    private val old by argument("OLD", help = "Old input file.")
                      .path(exists = true, folderOkay = false, readable = true, fileSystem = inputFs)
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun shouldNotReportUnusedWhenStaticImportIsFromAnotherPackage() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                package com.foo

                import android.text.Spannable
                import androidx.core.text.toSpannable

                fun foo(text: String): Spannable {
                    return text.toSpannable()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `should import alias after as and not when as is present in package name`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                package com.fastcompany.foo

                import com.fastcompany.common.Awesome as CommonAsset
                import com.company.common.alias as CommonAsset

                class SomeConfig {

                    fun fooFunction() = CommonAsset()
                }

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `should import alias after as and not when as is present in package ending name`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.company.common.alias as CommonAsset

                class SomeConfig {

                    fun fooFunction() = CommonAsset()
                }

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `if import path contains "import" word - does not report issues`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.pinterest.ktlint.ruleset.standard.internal.importordering.PatternEntry
                import com.pinterest.ktlint.ruleset.standard.internal.importordering.parseImportsLayout
                import org.assertj.core.api.Assertions.assertThat
                import org.junit.Test

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
            ).isEmpty()
        )
    }

    @Test
    fun `only redundant static imports should be removed`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.foo.psi.abc
                import com.foo.psi.findAnnotation

                fun main() {
                    val psi = getPsi()
                    psi.abc()
                    val bar = psi.findAnnotation(SOME_CONSTANT)
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `only redundant sealed sub class imports should be removed`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.foo.psi.Sealed
                import com.foo.psi.Sealed.SubClass

                fun main() {
                    listOf<Sealed>()
                    Sealed.SubClass()
                    SubClass()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `only redundant static java function imports should be removed`() {
        assertThat(
            NoUnusedImportsRule().lint(
                """
                import com.google.cloud.bigtable.data.v2.models.Mutation
                import com.google.cloud.bigtable.data.v2.models.Row
                import com.google.cloud.bigtable.data.v2.models.RowMutation.create

                fun test(row: Row) {
                    create("string", "string", Mutation.create())
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
