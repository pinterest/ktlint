package com.github.shyiko.ktlint

import com.github.shyiko.ktlint.io.Glob
import com.github.shyiko.ktlint.io.GlobFileFilter
import com.github.shyiko.ktlint.io.HiddenFileFilter
import com.github.shyiko.ktlint.io.allOf
import com.github.shyiko.ktlint.io.fromSlash
import com.github.shyiko.ktlint.io.slash
import org.kohsuke.args4j.Argument
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import org.kohsuke.args4j.ParserProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.concurrent.Callable
import kotlin.system.exitProcess

object Main {

    @Option(name="--format", aliases = arrayOf("-F"), usage = "Automatically format code")
    private var format: Boolean = false

    @Option(name="--verbose", aliases = arrayOf("-v"), usage = "Show error codes")
    private var verbose: Boolean = false

    @Option(name="--stdin", usage = "Read file text from stdin")
    private var stdin: Boolean = false

    @Option(name="--version", usage = "Version", help = true)
    private var version: Boolean = false

    @Option(name="--help", aliases = arrayOf("-h"), help = true)
    private var help: Boolean = false

    @Option(name="--debug", usage = "Turn on debug output")
    private var debug: Boolean = false

    @Argument
    private var patterns = ArrayList<String>()

    private fun CmdLineParser.usage(): String =
        """
        Kotlin Standard Style (https://github.com/shyiko/ktlint).

        Usage:
          ktlint <flags> [patterns]
          java -jar ktlint <flags> [patterns]

        Flags:
${ByteArrayOutputStream().let { this.printUsage(it); it }.toString().trimEnd().split("\n").map { "         $it" }
            .joinToString("\n")}

        Examples:
          # lint all **/*.kt and **/*.kts files starting from the current working directory.
          # NOTE: hidden directories (beginning with .) and files present in .gitignore will not be checked.
          ktlint

          # lint kotlin files under src/main/kotlin
          ktlint "src/main/kotlin/**/.kt{,s}"
        """.trimIndent()

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = CmdLineParser(this, ParserProperties.defaults()
            .withShowDefaults(false)
            .withUsageWidth(120)
            .withOptionSorter({ l, r ->
                l.option.toString().replace("-", "").compareTo(r.option.toString().replace("-", ""))
            }))
        try {
            parser.parseArgument(*args)
        } catch (err: CmdLineException) {
            System.err.println("Error: ${err.message}\n\n${parser.usage()}")
            exitProcess(1)
        }
        if (version) { println(javaClass.`package`.implementationVersion); exitProcess(0) }
        if (help) { println(parser.usage()); exitProcess(0) }
        val workDir = File(".").canonicalPath
        var tripped = false
        val start = System.currentTimeMillis()
        fun msg(fileName: String, e: Exception): String = when (e) {
            is ParseException -> {
                "$fileName:${e.line}:${e.col}: Not a valid Kotlin file (${e.message?.toLowerCase()})"
            }
            is RuleExecutionException -> {
                "$fileName:${e.line}:${e.col}: Internal Error (${e.ruleId}). " +
                    "Please create a ticket at https://github.com/shyiko/ktlint/issue " +
                    "(if possible, provide the source code that triggered an error)"
            }
            else -> throw e
        }
        fun apply(fileName: String, fileContent: String): List<String> {
            if (debug) {
                System.err.println("[DEBUG] Checking $fileName")
            }
            val result = ArrayList<String>()
            if (format) {
                val formattedFileContent = try { KtLint.format(fileContent) } catch (e: Exception) {
                    result.add(msg(fileName, e))
                    tripped = true
                    fileContent // making sure `cat file | ktlint --stdint > file` is (relatively) safe
                }
                if (stdin) {
                    println(formattedFileContent)
                } else {
                    if (fileContent !== formattedFileContent) {
                        File(fileName).writeText(formattedFileContent, charset("UTF-8"))
                    }
                }
            } else {
                try {
                    KtLint.lint(fileContent, { e ->
                        tripped = true
                        result.add("$fileName:${e.line}:${e.col}: " +
                            "${e.detail}${if (verbose) " (${e.id})" else ""}")
                    })
                } catch (e: Exception) {
                    result.add(msg(fileName, e))
                }
            }
            return result
        }
        var fileNumber = 0
        var errorNumber = 0
        fun process(dir: File, filter: FileFilter) = visit(dir, filter)
            .map { file -> Callable { apply(file.path, file.readText()) } }
            .parallel({
                fileNumber++
                errorNumber += it.size
                if (format) it.forEach { System.err.println(it) } else it.forEach { System.out.println(it) }
            })
        if (stdin) {
            apply("<text>", String(System.`in`.readBytes()))
        } else {
            if (patterns.isEmpty()) {
                val filter = allOf(HiddenFileFilter(false), GlobFileFilter(workDir, "**/*.kt"))
                process(File(workDir), filter)
            } else {
                val filter = GlobFileFilter(workDir, *patterns.toTypedArray())
                patterns
                    .map { Glob.prefix(slash(it)) }
                    .distinct()
                    .map { (if (it.startsWith("/")) File(fromSlash(it)) else
                        File(workDir, fromSlash(it))).canonicalPath }
                    // remove overlapping paths (e.g. /a & /a/b -> /a)
                    .sorted()
                    .fold(ArrayList<String>(), { r, v ->
                        if (r.isEmpty() || !v.startsWith(r.last())) { r.add(v) }; r })
                    .forEach { process(File(it), filter) }
            }
        }
        if (debug) {
            System.err.println("[DEBUG] ${(System.currentTimeMillis() - start)
                }ms / ${fileNumber}file(s) / ${errorNumber}error(s)")
        }
        if (tripped) {
            exitProcess(1)
        }
    }

    fun visit(dir: File, filter: FileFilter): Sequence<File> {
        val stack = ArrayDeque<File>().apply { push(dir) }
        return generateSequence(fun (): File? {
            while (true) {
                val file = stack.pollLast()
                if (file != null && file.isDirectory) {
                    stack.addAll(file.listFiles(filter))
                    continue
                }
                return file
            }
        })
    }

}
