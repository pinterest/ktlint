package com.pinterest.ktlint.rule.engine.core.api

/**
 * The version in which the rule was introduced.
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
