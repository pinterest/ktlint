package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.internal.EditorConfigDefaultsLoader
import java.nio.file.Path
import org.ec4j.core.model.EditorConfig

/**
 * Wrapper around the [EditorConfig]. Only to be used only for the default value of properties.
 */
public data class EditorConfigDefaults(public val value: EditorConfig) {
    public companion object {
        private val EDITOR_CONFIG_DEFAULTS_LOADER = EditorConfigDefaultsLoader()

        /**
         * Loads properties from [path]. [path] may either locate a file (also allows specifying a file with a name other
         * than ".editorconfig") or a directory in which a file with name ".editorconfig" is expected to exist. Properties
         * from all globs are returned.
         *
         * If [path] is not valid then the [EMPTY_EDITOR_CONFIG_DEFAULTS] is returned.
         *
         * The property "root" which denotes whether the parent directory is to be checked for the existence of a fallback
         * ".editorconfig" is ignored entirely.
         */
        public fun load(path: Path?): EditorConfigDefaults =
            if (path == null) {
                EMPTY_EDITOR_CONFIG_DEFAULTS
            } else {
                EDITOR_CONFIG_DEFAULTS_LOADER.load(path)
            }

        /**
         * Empty representation of [EditorConfigDefaults].
         */
        public val EMPTY_EDITOR_CONFIG_DEFAULTS: EditorConfigDefaults = EditorConfigDefaults(EditorConfig.builder().build())

        @Deprecated(
            message = "Marked for removal in KtLint 0.49",
            replaceWith = ReplaceWith("EMPTY_EDITOR_CONFIG_DEFAULTS"),
        )
        @Suppress("ktlint:experimental:property-naming")
        public val emptyEditorConfigDefaults: EditorConfigDefaults = EMPTY_EDITOR_CONFIG_DEFAULTS
    }
}
