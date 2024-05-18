package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.LintError

internal fun LintError.logMessage(code: Code) =
    "${code.fileNameOrStdin()}:$line:$col: $detail ($ruleId)" +
        if (canBeAutoCorrected) {
            ""
        } else {
            " [cannot be autocorrected]"
        }
