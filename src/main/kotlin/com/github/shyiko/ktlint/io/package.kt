package com.github.shyiko.ktlint.io

import java.io.File
import java.io.FileFilter

fun allOf(vararg filter: FileFilter) = FileFilter { file -> filter.all { it.accept(file) } }

/**
 * On Windows: "C:\file" -> "/C:/file", does nothing everywhere else.
 */
fun slash(path: String) = path.replace('\\', '/').let { if (!it.startsWith("/") && it.contains(":/")) "/$it" else it }
fun fromSlash(path: String) = path.replace('/', File.separatorChar)
    .let { if (it.contains(":/")) it.removePrefix("/") else it }
