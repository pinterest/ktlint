package com.pinterest.ktlint.test

public val debugAST: () -> Boolean = {
    (System.getProperty("ktlintDebug") ?: System.getenv("KTLINT_DEBUG") ?: "")
        .toLowerCase().split(",").contains("ast")
}

@Deprecated(
    message = "Moved to 'test' rulesets. This typealias will be removed in the future versions."
)
public typealias DumpAST = com.pinterest.ruleset.test.DumpASTRule
