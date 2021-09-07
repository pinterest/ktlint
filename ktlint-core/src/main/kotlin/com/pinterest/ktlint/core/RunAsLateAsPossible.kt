package com.pinterest.ktlint.core

/**
 * Defer the execution as much as possible. Note that multiple rules can be decorated with this annotation so it does
 * not guarantee that the annotated rule is executed as the last rule.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class RunAsLateAsPossible
