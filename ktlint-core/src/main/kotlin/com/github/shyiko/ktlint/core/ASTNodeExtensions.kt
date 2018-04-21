package com.github.shyiko.ktlint.core

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

fun ASTNode.visit(cb: (node: ASTNode) -> Unit) {
    cb(this)
    this.getChildren(null).forEach { it.visit(cb) }
}
