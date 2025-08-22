package com.pinterest.ktlint.rule.engine.core.api

/**
 * The version in which the rule was introduced.
 *
 * @param version The version in "major.minor" format (e.g., "1.2"), never including patch level
 * @param status The status of the rule in the specified version
 */
@Repeatable
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class SinceKtlint(
    val version: String,
    val status: Status,
) {
    public enum class Status { STABLE, EXPERIMENTAL }
}
