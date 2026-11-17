package com.spcode.tnpscprep.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun empty_or_null_means_follow_the_system() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
    }

    @Test fun known_tags_map_to_languages() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.TAMIL, AppLanguage.fromTag("ta"))
    }

    @Test fun regional_variants_and_lists_use_the_primary_language() {
        assertEquals(AppLanguage.TAMIL, AppLanguage.fromTag("ta-IN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("EN-gb"))
        assertEquals(AppLanguage.TAMIL, AppLanguage.fromTag("ta-IN,en-US")) // first of a list
    }

    @Test fun unsupported_language_falls_back_to_system() = assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("hi"))

    @Test fun round_trip_of_tags() = AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
}
