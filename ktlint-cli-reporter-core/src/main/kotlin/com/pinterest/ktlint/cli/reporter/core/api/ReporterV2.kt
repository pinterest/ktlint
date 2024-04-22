package com.pinterest.ktlint.cli.reporter.core.api

/**
 * Implementation must be thread-safe. In particular, [onLintError] might be called in parallel for
 * different files (but not for the same file) ([before], [onLintError] and [after] are guaranteed to be called
 * on the same thread).
 * @see ReporterProvider
 */
public interface ReporterV2 {
    /**
     * This function is called once, before the processing begins (regardless of whether any files matching the pattern are (going to)
     * found). It's guaranteed to be called before any of the other [ReporterV2]s methods.
     */
    public fun beforeAll() {}

    /**
     * This function is called once for each file (matching the pattern) found, but before it's parsed.
     */
    public fun before(file: String) {}

    /**
     * This function is called once for each lint error that is found.
     */
    public fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    )

    /**
     * This function is called once after the file has been parsed entirely.
     */
    public fun after(file: String) {}

    /**
     * This function is called once, after all the files (if any) have been processed. It's guaranteed to be called after all other
     * [ReporterV2]s methods.
     */
    public fun afterAll() {}
}
