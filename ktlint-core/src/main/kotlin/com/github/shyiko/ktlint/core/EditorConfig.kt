package com.github.shyiko.ktlint.core

interface EditorConfig {

    companion object {
        fun fromMap(map: Map<String, String>) = object : EditorConfig {
            override fun get(key: String): String? = map[key]
        }
    }

    /**
     * @see KtLint.EDITOR_CONFIG_USER_DATA_KEY
     * @see KtLint.ANDROID_USER_DATA_KEY
     */
    fun get(key: String): String?
}
