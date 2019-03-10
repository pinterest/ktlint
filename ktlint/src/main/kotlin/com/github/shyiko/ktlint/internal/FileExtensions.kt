package com.github.shyiko.ktlint.internal

import java.io.File
import java.io.IOException

internal fun File.mkdirsOrFail() {
    if (!mkdirs() && !isDirectory) {
        throw IOException("Unable to create \"${this}\" directory")
    }
}
