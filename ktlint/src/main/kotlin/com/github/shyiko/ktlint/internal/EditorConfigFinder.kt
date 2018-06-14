package com.github.shyiko.ktlint.internal

import java.io.File

class EditorConfigFinder(private val workDir: String, private val debug: Boolean) {

    private val editorConfigs = mutableMapOf<String, EditorConfig?>()

    init {
        editorConfigs[workDir] = EditorConfig.of(workDir)
    }

    @Synchronized
    fun getEditorConfig(file: File): EditorConfig? = editorConfigs.getOrPut(file.key(), {
        findParentEditorConfig(file)
    })

    @Synchronized
    fun getEditorConfig(filePath: String): EditorConfig? = getEditorConfig(File(filePath))

    private fun findParentEditorConfig(file: File): EditorConfig? {
        var currentFile: File = if (file.isDirectory) file else file.parentFile
        val pathsWithMissingEditorConfig = mutableListOf<String>()

        while (currentFile.canonicalPath != workDir) {
            val currentPath = currentFile.canonicalPath
            val editorConfigFile = File("$currentPath/.editorconfig")
            if (editorConfigFile.exists() && editorConfigFile.isFile) {
                if (debug) {
                    System.err.println("[DEBUG] Found .editorconfig for ${file.name} in ${editorConfigFile.canonicalPath}")
                }

                return EditorConfig.of(currentPath)?.also {
                    pathsWithMissingEditorConfig.forEach { path ->
                        editorConfigs[path] = it
                    }
                }
            }

            pathsWithMissingEditorConfig.add(currentPath)
            currentFile = currentFile.parentFile
        }

        return editorConfigs[workDir]?.also {
            pathsWithMissingEditorConfig.forEach { path ->
                editorConfigs[path] = it
            }
        }
    }

    private fun File.key() = this.parent
}
