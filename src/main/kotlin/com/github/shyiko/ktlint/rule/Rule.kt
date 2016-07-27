package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Implementation is required to be neither thread-safe nor stateless.
 */
interface Rule { fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) }
