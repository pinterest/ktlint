package com.pinterest.ktlint.reporter.format.internal

/**
 * Stripped down version of https://github.com/ziggy42/kolor (ziggy42/kolor#6).
 */

@Deprecated("Marked for removal from public API in KtLint 0.48. Please raise an issue if you have a use case to keep it public.")
public fun String.color(foreground: Color): String =
    "\u001B[${foreground.code}m$this\u001B[0m"

@Deprecated("Marked for removal from public API in KtLint 0.48. Please raise an issue if you have a use case to keep it public.")
public enum class Color(public val code: Int) {
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    LIGHT_GRAY(37),
    DARK_GRAY(90),
    LIGHT_RED(91),
    LIGHT_GREEN(92),
    LIGHT_YELLOW(93),
    LIGHT_BLUE(94),
    LIGHT_MAGENTA(95),
    LIGHT_CYAN(96),
    WHITE(97),
}
