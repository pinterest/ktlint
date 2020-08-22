fun f() {
    val y = 5
    val x =
        """
            $y
        """.trimIndent()
    println("""${true}""".trimIndent())
    println(
        """
        """.trimIndent()
    )
    println(
        """
    ${true}

        ${true}
        """.trimIndent()
    )
    println(
        """
${true}

    ${true}
        """.trimIndent()
    )
    println(
        """
    text

        text
        """.trimIndent().toByteArray()
    )
    println(
        """
    text

        text
        """.trimIndent()
    )
    println(
        """
    text

        text
_
        """.trimIndent()
    )
    println(
        """
    text ""

        text
        """.trimIndent(),
        ""
    )
    format(
        """
            class A {
                fun f(@Annotation
                      a: Any,
                      @Annotation([
                          "v1",
                          "v2"
                      ])
                      b: Any,
                      c: Any =
                          false,
                      @Annotation d: Any) {
                }
            }
        """.trimIndent()
    )
    write(
        fs.getPath("/projects/.editorconfig"),
        """
        root = true
        [*]
        end_of_line = lf
        """.trimIndent().toByteArray()
    )
    SpacingAroundKeywordRule().format( // string below is tab-indented
        """
            var x: String
			    get () {
				    return ""
			    }
			    private set (value) {
				    x = value
			    }
            """.trimIndent()
    )
}

class C {
    val CONFIG_COMPACT = """
        {
        }
    """.trimIndent()
    val CONFIG_COMPACT = // comment
        """
        {
        }
        """.trimIndent()

    fun getBazelWorkspaceContent(blueprint: BazelWorkspaceBlueprint) =
        """${Target(
            "android_sdk_repository",
            listOf(StringAttribute("name", "androidsdk"))
        )}

${Comment("Google Maven Repository")}
${LoadStatement("@bazel_tools//tools/build_defs/repo:http.bzl", listOf("http_archive"))}
${AssignmentStatement("GMAVEN_TAG", "\"${blueprint.gmavenRulesTag}\"")}
${Target(
            "http_archive",
            listOf(
                StringAttribute("name", "gmaven_rules"),
                RawAttribute("strip_prefix", "\"gmaven_rules-%s\" % GMAVEN_TAG"),
                RawAttribute("urls", "[\"https://github.com/bazelbuild/gmaven_rules/archive/%s.tar.gz\" % GMAVEN_TAG]")
            )
        )}
${LoadStatement("@gmaven_rules//:gmaven.bzl", listOf("gmaven_rules"))}
${Target("gmaven_rules", listOf())}
"""

}
