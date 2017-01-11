package com.github.shyiko.ktlint.internal.path

import java.io.File
import java.io.FileFilter

fun FileFilter.and(fileFilter: FileFilter) =
    FileFilter { file -> this@and.accept(file) && fileFilter.accept(file) }
fun FileFilter.or(fileFilter: FileFilter) =
    FileFilter { file -> this@or.accept(file) || fileFilter.accept(file) }

/**
 * On Windows: "C:\io" -> "/C:/io", does nothing everywhere else.
 */
fun slash(path: String) = path.replace('\\', '/').let { if (!it.startsWith("/") && it.contains(":/")) "/$it" else it }
fun fromSlash(path: String) = path.replace('/', File.separatorChar)
    .let { if (it.contains(":/")) it.removePrefix("/") else it }

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
fun expandTilde(path: String) = path.replaceFirst(Regex("^~"), System.getProperty("user.home"))
