package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.LintError

internal fun <T> lintErrorLineAndColumnComparator(transformer: (T) -> LintError) =
    compareBy<T> { transformer(it).line }
        .then(compareBy { transformer(it).col })
