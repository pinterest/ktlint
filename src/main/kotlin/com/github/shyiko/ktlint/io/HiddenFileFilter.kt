package com.github.shyiko.ktlint.io

import java.io.File
import java.io.FileFilter

class HiddenFileFilter(private val hidden: Boolean = true) : FileFilter {

    override fun accept(file: File): Boolean = file.isHidden == hidden

}
