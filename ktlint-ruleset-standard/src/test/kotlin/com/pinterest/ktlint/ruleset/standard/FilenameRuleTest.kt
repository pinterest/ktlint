package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilenameRuleTest {
    @Test
    fun testParsingAllTopLevelDeclarations() {
        data class Item(val src: String, val type: String, val typeName: String, val fileName: String)

        for (item in listOf(
            Item("class AClass", "Class", "AClass", "AClass"),
            Item("class `AClass`", "Class", "`AClass`", "AClass"),
            Item("interface AInterface", "Class", "AInterface", "AInterface"),
            Item("interface `AInterface`", "Class", "`AInterface`", "AInterface"),
            Item("data class ADataClass(val v: Int)", "Class", "ADataClass", "ADataClass"),
            Item("data class `ADataClass`(val v: Int)", "Class", "`ADataClass`", "ADataClass"),
            Item("sealed class ASealedClass", "Class", "ASealedClass", "ASealedClass"),
            Item("sealed class `ASealedClass`", "Class", "`ASealedClass`", "ASealedClass"),
            Item("sealed interface ASealedInterface", "Class", "ASealedInterface", "ASealedInterface"),
            Item("sealed interface `ASealedInterface`", "Class", "`ASealedInterface`", "ASealedInterface"),
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
               """, "Property", "propA", "propA"
            ),
            Item(
                """
               public val `propB`: Int
                   get() = 5
               """, "Property", "`propB`", "propB"
            ),
            Item(
                """
               public var propC: Int = 10
                   get() = 5
                   set(value) {
                       println(value)
                       field = value
                   }
               """, "Property", "propC", "propC"
            ),
            Item(
                """
               public var `propD`: Int = 10
                   get() = 5
                   set(value) {
                       println(value)
                       field = value
                   }
               """, "Property", "`propD`", "propD"
            ),
            Item(
                """
               public val Int.propE: Int
                   get() = this * 5
               """, "Extension property", "propE", "propE"
            ),
            Item(
                """
               public val Int.`propF`: Int
                   get() = this * 5
               """, "Extension property", "`propF`", "propF"
            ),
            Item(
                """
               public val `Int`.propG: Int
                   get() = this * 5
               """, "Extension property", "propG", "propG"
            ),
            Item(
                """
               public var Int.propH: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
               """, "Extension property", "propH", "propH"
            ),
            Item(
                """
               public var Int.`propI`: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
               """, "Extension property", "`propI`", "propI"
            ),
            Item(
                """
               public var `Int`.propJ: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
               """, "Extension property", "propJ", "propJ"
            ),
            Item(
                """
               public val <T : Number> T.propK: Int
                   get() = this * 5
               """, "Extension property", "propK", "propK"
            ),
            Item(
                """
               public var <T : Number> T.propL: Int
                   get() = this * 5
                   set(value) {
                       println(this * value)
                   }
               """, "Extension property", "propL", "propL"
            ),
            Item("typealias NodeSet = Set<Network.Node>", "Typealias", "NodeSet", "NodeSet"),
            Item("typealias FileTable<K> = MutableMap<K, MutableList<File>>", "Typealias", "FileTable", "FileTable"),
            Item("typealias MyHandler = (Int, String, Any) -> Unit", "Typealias", "MyHandler", "MyHandler"),
            Item("typealias Predicate<T> = (T) -> Boolean", "Typealias", "Predicate", "Predicate")
        )) {
            val (src, type, typeName, fileName) = item
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
        for (
        src in listOf(
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
        )
        ) {
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
            ).isEmpty()
        }
    }

    @Test
    fun testNonMatchingSingleClassName() {
        for (
        src in mapOf(
            "class A" to "Class",
            "data class A(val v: Int)" to "Class",
            "sealed class A" to "Class",
            "interface A" to "Class",
            "object A" to "Object",
            "enum class A {A}" to "Class",
            "typealias A = Set<Network.Node>" to "Typealias"
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "/some/path/B.kt",
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
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "${src.value} A should be declared in a file named A.kt")
                )
            )
        }
    }

    @Test
    fun testFileWithoutTopLevelDeclarations() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                /*
                 * copyright
                 */
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testMultipleTopLevelClasses() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                class B
                class C
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testNonMatchingMultipleElementsWithNonPascalCaseFilename() {
        for (
        src in listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "foo.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent(),
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
        for (src in listOf(
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
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "A.kt",
                    """
                    $src
                    """.trimIndent(),
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMatchingReceiverFilenameWithMultipleElementsButWithDifferentElement() {
        for (src in listOf(
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
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "non-matching-file-name.kt",
                    """
                        data class Foo(val value: String)
                        $src
                    """.trimIndent(),
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
        for (src in listOf(
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
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "foo.kt",
                    """
                    $src
                    """.trimIndent(),
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
        for (
        src in listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "Foo.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent(),
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMultipleElementsInPackageKtFile() {
        for (
        src in listOf(
            "class A",
            "class `A`",
            "data class A(val v: Int)",
            "sealed class A",
            "interface A",
            "object A",
            "enum class A {A}",
            "typealias A = Set<Network.Node>",
            "fun A.f() {}"
        )
        ) {
            assertThat(
                FilenameRule().lint(
                    "package.kt",
                    """
                    class Bar
                    $src
                    """.trimIndent(),
                )
            ).isEmpty()
        }
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                class B {
                    class C
                    class D
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "Class B should be declared in a file named B.kt")
            )
        )
    }

    @Test
    fun testCaseSensitiveMatching() {
        assertThat(
            FilenameRule().lint(
                "woohoo.kt",
                """
                interface Woohoo
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "Class Woohoo should be declared in a file named Woohoo.kt")
            )
        )
    }

    @Test
    fun testCaseEscapedClassNames() {
        assertThat(
            FilenameRule().lint(
                "B.kt",
                """
                class `A`
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "Class `A` should be declared in a file named A.kt")
            )
        )
    }

    @Test
    fun testIgnoreKotlinScriptFiles() {
        assertThat(
            FilenameRule().lint(
                "A.kts",
                """
                class B
                """.trimIndent()
            )
        ).isEmpty()
    }
}
