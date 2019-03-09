data class BuildSystemConfig(
    var buildSystemVersion: String?,
/*
        var agpVersion: String?,
*/
    var kotlinVersion: String?,
    var generateBazelFiles: Boolean?,

    /**
     * Allows to set different properties in "gradle.properties" file.
     * Defaults:
     * org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError
     * org.gradle.daemon=true
     * org.gradle.parallel=true
     * org.gradle.caching=true
     */
    var properties: Map<String, String>?
)
