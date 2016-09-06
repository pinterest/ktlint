package com.github.shyiko.ktlint.internal.path

import java.io.File
import java.io.FileFilter

class HiddenFileFilter(private val reverse: Boolean = false) : FileFilter {

    override fun accept(file: File): Boolean = file.isHidden != reverse

}
