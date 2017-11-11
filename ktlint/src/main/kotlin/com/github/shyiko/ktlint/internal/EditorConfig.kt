package com.github.shyiko.ktlint.internal

import org.ini4j.Wini
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class EditorConfig private constructor (
    val path: Path,
    private val data: Map<String, String>
) : Map<String, String> by data {

    companion object {

        fun of(dir: String) =
            of(Paths.get(dir))
        fun of(dir: Path) =
            locate(dir)?.let { EditorConfig(it, load(it)) }

        private fun locate(dir: Path?): Path? = when (dir) {
            null -> null
            else -> Paths.get(dir.toString(), ".editorconfig").let {
                if (Files.exists(it)) it else locate(dir.parent)
            }
        }

        private fun load(path: Path): Map<String, String> {
            val editorConfig = Wini(ByteArrayInputStream(Files.readAllBytes(path)))
            // right now ktlint requires explicit [*.{kt,kts}] section
            // (this way we can be sure that users want .editorconfig to be recognized by ktlint)
            val section = editorConfig["*.{kt,kts}"]
            return section?.toSortedMap() ?: emptyMap()
        }
    }
}
