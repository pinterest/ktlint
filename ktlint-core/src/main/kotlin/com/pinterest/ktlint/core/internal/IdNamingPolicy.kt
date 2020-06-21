package com.pinterest.ktlint.core.internal

/**
 * Provides policy to have consistent and restricted `id` field naming style.
 */
internal object IdNamingPolicy {
    private const val ID_REGEX = "[a-z]+([-][a-z]+)*"
    private val idRegex = ID_REGEX.toRegex()

    /**
     * Checks provided [id] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [id] name.
     */
    internal fun enforceNaming(id: String) =
        require(id.matches(idRegex)) { "id $id must match $ID_REGEX" }
}
