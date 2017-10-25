package com.github.shyiko.ktlint.core

interface EditorConfig {

    companion object {
        fun fromMap(map: Map<String, String>) = object : EditorConfig {
            override fun get(key: String): String? = map[key]
        }
    }

    fun get(key: String): String?
}
