package com.pinterest.ktlint.ruleset.standard.internal

import java.text.Normalizer

/**
 * Removes diacritics from letters. Note that ligatures æ (ae), œ (oe), Æ (AE), Œ (OE), and letters with strokes ł (l),
 * ø (o), ß (s), Ł (L), Ø (O) are not changed.
 */

internal fun String.removeDiacriticsFromLetters() =
    map { originalChar ->
        Normalizer
            // Decompose characters having a diacritic into an ascii alphabetic character (a-zA-Z) followed by the diacritic(s)
            .normalize(originalChar.toString(), Normalizer.Form.NFD)
            .let {
                if (it.first().isLetterOrDigit()) {
                    // Ignore all diacritics
                    it.first()
                } else {
                    it
                }
            }
    }.joinToString(separator = "")
