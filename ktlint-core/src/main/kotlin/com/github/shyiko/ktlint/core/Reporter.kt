package com.github.shyiko.ktlint.core

/**
 * Implementation must be thread-safe. In particular, [onLintError] might be called in parallel for
 * different files (but not for the same file) ([before], [onLintError] and [after] are guarantied to be called
 * on the same thread).
 * @see ReporterProvider
 */
interface Reporter {

    /**
     * Called once, before the processing begins
     * (regardless of whether any files matching the pattern are (going to) found).
     * It's guarantied to be called before any of the other [Reporter]s methods.
     */
    fun beforeAll() {}
    /**
     * Called when file (matching the pattern) is found but before it's parsed.
     */
    fun before(file: String) {}
    fun onLintError(file: String, err: LintError, corrected: Boolean)
    /**
     * Called after ktlint is done with the file.
     */
    fun after(file: String) {}
    /**
     * Called once, after all the files (if any) have been processed.
     * It's guarantied to be called after all other [Reporter]s methods.
     */
    fun afterAll() {}

}
