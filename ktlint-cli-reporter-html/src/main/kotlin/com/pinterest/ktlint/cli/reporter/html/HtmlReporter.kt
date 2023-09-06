/*
 * MIT License
 *
 * Copyright (c) 2019 Matheus Candido
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pinterest.ktlint.cli.reporter.html

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

public class HtmlReporter(
    private val out: PrintStream,
) : ReporterV2 {
    private val acc = ConcurrentHashMap<String, MutableList<KtlintCliError>>()
    private var issueCount = 0
    private var correctedCount = 0

    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        if (ktlintCliError.status != FORMAT_IS_AUTOCORRECTED) {
            issueCount += 1
            acc.getOrPut(file) { mutableListOf() }.add(ktlintCliError)
        } else {
            correctedCount += 1
        }
    }

    override fun afterAll() {
        html {
            head {
                cssLink("https://fonts.googleapis.com/css?family=Source+Code+Pro")
                text("<meta http-equiv=\"Content-Type\" Content=\"text/html; Charset=UTF-8\">${System.lineSeparator()}")
                text("<style>${System.lineSeparator()}")
                text("body {${System.lineSeparator()}")
                text("    font-family: 'Source Code Pro', monospace;${System.lineSeparator()}")
                text("}${System.lineSeparator()}")
                text("h3 {${System.lineSeparator()}")
                text("    font-size: 12pt;${System.lineSeparator()}")
                text("}")
                text("</style>${System.lineSeparator()}")
            }
            body {
                if (!acc.isEmpty()) {
                    h1 { text("Overview") }

                    paragraph {
                        text("Issues found: $issueCount")
                    }

                    paragraph {
                        text("Issues corrected: $correctedCount")
                    }

                    acc.forEach { (file: String, ktlintCliErrors: MutableList<KtlintCliError>) ->
                        h3 { text(file) }
                        ul {
                            ktlintCliErrors.forEach { err ->
                                with(err) {
                                    item("($line, $col): $detail  ($ruleId)")
                                }
                            }
                        }
                    }
                } else {
                    paragraph {
                        text("Congratulations, no issues found!")
                    }
                }
            }
        }
    }

    private fun html(body: () -> Unit) {
        out.println("<html>")
        body()
        out.println("</html>")
    }

    private fun head(body: () -> Unit) {
        out.println("<head>")
        body()
        out.println("</head>")
    }

    private fun body(body: () -> Unit) {
        out.println("<body>")
        body()
        out.println("</body>")
    }

    private fun h1(body: () -> Unit) {
        out.print("<h1>")
        body()
        out.println("</h1>")
    }

    private fun h3(body: () -> Unit) {
        out.print("<h3>")
        body()
        out.println("</h3>")
    }

    private fun text(value: String) {
        out.print(value)
    }

    private fun ul(body: () -> Unit) {
        out.println("<ul>")
        body()
        out.println("</ul>")
    }

    private fun item(value: String) {
        out.print("<li>")
        text(value.escapeHTMLAttrValue())
        out.println("</li>")
    }

    private fun cssLink(link: String) {
        out.print("<link href=\"")
        out.print(link)
        out.println("\" rel=\"stylesheet\" />")
    }

    private fun paragraph(body: () -> Unit) {
        out.print("<p>")
        body()
        out.println("</p>")
    }

    private fun String.escapeHTMLAttrValue() =
        this
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
