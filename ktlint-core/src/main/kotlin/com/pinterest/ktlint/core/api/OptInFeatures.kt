package com.pinterest.ktlint.core.api

@RequiresOptIn(
    message = "This Ktlint feature is highly experimental, and most probably will change in the future releases.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
public annotation class FeatureInAlphaState

@RequiresOptIn(
    message = "This Ktlint feature is experimental, and may change in the future releases."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
public annotation class FeatureInBetaState
