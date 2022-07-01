package com.pinterest.ktlint.ruleset.standard.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class RemoveDiacriticsFromLettersTest {
    @ParameterizedTest(name = "Original character: {0}, expected result: {1}")
    @CsvSource(
        value = [
            "àáâäãåā,aaaaaaa",
            "çćč,ccc",
            "èéêëēėę,eeeeeee",
            "îïíīįì,iiiiii",
            "ñń,nn",
            "ôöòóōõ,oooooo",
            "śš,ss",
            "ûüùúū,uuuuu",
            "ÿ,y",
            "žźż,zzz",
            "ÀÁÂÄÃÅĀ,AAAAAAA",
            "ÇĆČ,CCC",
            "ÈÉÊËĒĖĘ,EEEEEEE",
            "ÎÏÍĪĮÌ,IIIIII",
            "ÑŃ,NN",
            "ÔÖÒÓŌÕ,OOOOOO",
            "ŚŠ,SS",
            "ÛÜÙÚŪ,UUUUU",
            "Ÿ,Y",
            "ŽŹŻ,ZZZ"
        ]
    )
    fun `Given a letter with a diacritic then remove it`(original: String, expected: String) {
        assertThat(original.removeDiacriticsFromLetters()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Character: {0}")
    @ValueSource(
        strings = [
            "æ", "ł", "œ", "ø", "ß", "Æ", "Ł", "Œ", "Ø"
        ]
    )
    fun `Given a ligature or letter with stroke then keep it unchanged`(original: String) {
        assertThat(original.removeDiacriticsFromLetters()).isEqualTo(original)
    }

    @Test
    fun `Given a string containing`() {
        assertThat("ÅÄÖāăąēîïĩíĝġńñšŝśûůŷ".removeDiacriticsFromLetters()).isEqualTo("AAOaaaeiiiiggnnsssuuy")
    }
}
