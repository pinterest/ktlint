package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilenameRuleTest {
    private val fileNameRuleAssertThat = FilenameRule().assertThat()

    @Test
    fun testParsingAllTopLevelDeclarations() {
        data class Item(val src: String, val type: String, val typeName: String, val fileName: String)

        listOf(
            Item("class AClass", "Class", "AClass", "AClass"),
            Item("class `AClass`", "Class", "`AClass`", "AClass"),
            Item("interface AInterface", "Interface", "AInterface", "AInterface"),
            Item("interface `AInterface`", "Interface", "`AInterface`", "AInterface"),
            Item("data class ADataClass(val v: Int)", "Class", "ADataClass", "ADataClass"),
            Item("data class `ADataClass`(val v: Int)", "Class", "`ADataClass`", "ADataClass"),
            Item("sealed class ASealedClass", "Class", "ASealedClass", "ASealedClass"),
            Item("sealed class `ASealedClass`", "Class", "`ASealedClass`", "ASealedClass"),
            Item("sealed interface ASealedInterface", "Interface", "ASealedInterface", "ASealedInterface"),
            Item("sealed interface `ASealedInterface`", "Interface", "`ASealedInterface`", "ASealedInterface"),
            Item("enum class AEnum {A}", "Class", "AEnum", "AEnum"),
            Item("enum class `AEnum` {A}", "Class", "`AEnum`", "AEnum"),
            Item("object AObject {}", "Object", "AObject", "AObject"),
            Item("object `AObject` {}", "Object", "`AObject`", "AObject"),
            Item("fun aFun() = false", "Function", "aFun", "aFun"),
            Item("fun `aFun`() = false", "Function", "`aFun`", "aFun"),
            Item("fun Int.aFun() = false", "Extension function", "aFun", "aFun"),
            Item("fun <T : Number> T.aFun() = false", "Extension function", "aFun", "aFun"),
            Item("fun `Int`.aFun() = false", "Extension function", "aFun", "aFun"),
            Item("fun Int.`a Fun`() = false", "Extension function", "`a Fun`", "a Fun"),
            Item("fun `Int`.`a Fun`() = false", "Extension function", "`a Fun`", "a Fun"),
            Item("fun kotlin.Int.aFun() = false", "Extension function", "aFun", "aFun"),
            Item("fun `kotlin.Int`.aFun() = false", "Extension function", "aFun", "aFun"),
            Item(
                """
                public val propA: Int
                   get() = 5
                """,
                "Property",
                "propA",
                "propA"
            ),
            Item(
                """
                public val `propB`: Int
                   get() = 5
                """,
                "Property",
                "`propB`",
                "propB"
            ),
            Item(
                """
                public var propC: Int = 10
                   get() = 5
                   set(value) {
                       println(value)
                       field = value
                   }
                """,
                "Property",
                "propC",
                "propC"
            ),
            Item(
                """
                public var `propD`: Int = 10
                   get() = 5
                   set(value) {
                       println(value)
                       field = value
                   }
                """,
                "Property",
                "`propD`",
                "propD"
            ),
            Item(
                """
                public val Int.propE: Int
                   get() = this * 5
                """,
                "Extension property",
                "propE",
                "propE"
            ),
            Item(
                """
                public val Int.`propF`: Int
                   get() = this * 5
                """,
                "Extension property",
                "`propF`",
                "propF"
            ),
            Item(
                """
                public val `Int`.propG: Int
                   get() = this * 5
                """,
                "Extension property",
                "propG",
                "propG"
            ),
            Item(
                """
                public var Int.propH: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
                """,
                "Extension property",
                "propH",
                "propH"
            ),
            Item(
                """
                public var Int.`propI`: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
                """,
                "Extension property",
                "`propI`",
                "propI"
            ),
            Item(
                """
                public var `Int`.propJ: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
                """,
                "Extension property",
                "propJ",
                "propJ"
            ),
            Item(
                """
                public val <T : Number> T.propK: Int
                   get() = this * 5
                """,
                "Extension property",
                "propK",
                "propK"
            ),
            Item(
                """
                public var <T : Number> T.propL: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
                """,
                "Extension property",
                "propL",
                "propL"
            ),
            Item("typealias NodeSet = Set<Network.Node>", "Typealias", "NodeSet", "NodeSet"),
            Item("typealias FileTable<K> = MutableMap<K, MutableList<File>>", "Typealias", "FileTable", "FileTable"),
            Item("typealias MyHandler = (Int, String, Any) -> Unit", "Typealias", "MyHandler", "MyHandler"),
            Item("typealias Predicate<T> = (T) -> Boolean", "Typealias", "Predicate", "Predicate")
        ).forEach { (src, type, typeName, fileName) ->
            assertThat(
                FilenameRule().lint(
                    "/some/path/A.kt",
                    """
                    /*
                     * license
                     */
                    @file:JvmName("Foo")
                    package x
                    import y.Z
                    $src
                    //
                    """.trimIndent()
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "$type $typeName should be declared in a file named $fileName.kt")
                )
            )
        }
    }

    @Test
    fun testMatchingSingleClassName() {
        for (src in listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            // >1 declaration case
            "class B\nfun A.f() {}"
        )) {
            val code =
                """
                /*
                 * license
                 */
                @file:JvmName("Foo")
                package x
                import y.Z
                $src
                //
                """.trimIndent()
            fileNameRuleAssertThat(code)
                .asFileWithPath("/some/path/A.kt")
                .hasNoLintViolations()
        }
    }

    @Test
    fun testNonMatchingSingleClassName() {
        for (src in mapOf(
            "class A" to "Class",
            "data class A(val v: Int)" to "Class",
            "sealed class A" to "Class",
            "interface A" to "Interface",
            "object A" to "Object",
            "enum class A {A}" to "Class",
            "typealias A = Set<Network.Node>" to "Typealias"
        )) {
            val code =
                """
                /*
                 * license
                 */
                @file:JvmName("Foo")
                package x
                import y.Z
                ${src.key}
                //
                """.trimIndent()
            fileNameRuleAssertThat(code)
                .asFileWithPath("/some/path/UnexpectedFilename.kt")
                .hasLintViolationWithoutAutoCorrect(1, 1, "${src.value} A should be declared in a file named A.kt")
        }
    }

    @Test
    fun testFileWithoutTopLevelDeclarations() {
        val code =
            """
            /*
             * copyright
             */
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasNoLintViolations()
    }

    @Test
    fun testMultipleTopLevelClasses() {
        val code =
            """
            class B
            class C
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasNoLintViolations()
    }

    @Test
    fun testNonMatchingMultipleElementsWithNonPascalCaseFilename() {
        listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "foo.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent()
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "File name foo.kt should conform PascalCase")
                )
            )
        }
    }

    @Test
    fun testMatchingReceiverFilenameWithMultipleElements() {
        listOf(
            """
            fun A.f1() {}
            fun A.f2() {}
            """.trimIndent(),
            """
            public val A.propG: Int
               get() = this * 5

            public var A.propH: Int
               get() = this * 5
               set(value) {
                   println(this * value)
               }
            """.trimIndent()
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "A.kt",
                    """
                    $src
                    """.trimIndent()
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMatchingReceiverFilenameWithMultipleElementsButWithDifferentElement() {
        listOf(
            """
            fun A.f1() {}
            fun A.f2() {}
            """.trimIndent(),
            """
            public val A.propG: Int
               get() = this * 5

            public var A.propH: Int
               get() = this * 5
               set(value) {
                   println(this * value)
               }
            """.trimIndent(),
            """
            fun A.f1() {}
            public val A.propG: Int
               get() = this * 5
            """.trimIndent()
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "non-matching-file-name.kt",
                    """
                        data class Foo(val value: String)
                        $src
                    """.trimIndent()
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "File name non-matching-file-name.kt should conform PascalCase")
                )
            )
        }
    }

    @Test
    fun testNonMatchingReceiverFilenameWithMultipleElements() {
        listOf(
            """
            fun A.f1() {}
            fun A.f2() {}
            """.trimIndent(),
            """
            public val A.propG: Int
               get() = this * 5

            public var A.propH: Int
               get() = this * 5
               set(value) {
                   println(this * value)
               }
            """.trimIndent(),
            """
            fun A.f1() {}
            public val A.propG: Int
               get() = this * 5
            """.trimIndent()
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "foo.kt",
                    """
                    $src
                    """.trimIndent()
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "All elements with receiver A should be declared in a file named A.kt")
                )
            )
        }
    }

    @Test
    fun testMultipleElementsWithNonPascalCaseFilename() {
        listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "Foo.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent()
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMultipleElementsInPackageKtFile() {
        listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        ).forEach { src ->
            assertThat(
                FilenameRule().lint(
                    "package.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent()
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        val code =
            """
            class B {
                class C
                class D
            }
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "Class B should be declared in a file named B.kt")
    }

    @Test
    fun testCaseSensitiveMatching() {
        val code =
            """
            interface Woohoo
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("woohoo.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "Interface Woohoo should be declared in a file named Woohoo.kt")
    }

    @Test
    fun testCaseEscapedClassNames() {
        val code =
            """
            class `A`
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("B.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "Class `A` should be declared in a file named A.kt")
    }

    @Test
    fun testIgnoreKotlinScriptFiles() {
        val code =
            """
            class B
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("A.kts")
            .hasNoLintViolations()
    }
}
