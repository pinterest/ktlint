package com.pinterest.ktlint.core

import org.jetbrains.kotlin.com.intellij.lang.FileASTNode

/**
 * Run the annotated rule only on the root ([FileASTNode]) node of each file. In other words, [Rule.visit] will be
 * called on [FileASTNode] but not on [FileASTNode] children.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class RunOnRootNodeOnly
