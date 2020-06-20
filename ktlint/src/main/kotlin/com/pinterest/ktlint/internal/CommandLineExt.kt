package com.pinterest.ktlint.internal

import kotlin.system.exitProcess
import picocli.CommandLine

/**
 * Check if user requested either help or version options, if yes - print it
 * and exit process with [exitCode] exit code.
 */
internal fun CommandLine.printHelpOrVersionUsage(
    exitCode: Int = 0
) {
    if (isUsageHelpRequested) {
        usage(System.out, CommandLine.Help.Ansi.OFF)
        exitProcess(exitCode)
    } else if (isVersionHelpRequested) {
        printVersionHelp(System.out, CommandLine.Help.Ansi.OFF)
        exitProcess(exitCode)
    }
}
