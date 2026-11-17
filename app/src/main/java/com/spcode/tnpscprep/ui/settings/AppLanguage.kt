package com.spcode.tnpscprep.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** SYSTEM = no override: follow the phone's language. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""), ENGLISH("en"), TAMIL("ta");

    companion object {
        /** Accepts "ta", "ta-IN", "" or null; anything we don't ship counts as SYSTEM. */
        fun fromTag(tag: String?): AppLanguage {
            val lang = tag?.substringBefore(',')?.substringBefore('-')?.lowercase().orEmpty()
            return entries.firstOrNull { it.tag.isNotEmpty() && it.tag == lang } ?: SYSTEM
        }
    }
}

/**
 * Thin wrapper over AppCompat per-app locales. Setting a locale recreates the Activity;
 * ViewModels (and so the running timer) survive that.
 */
object AppLocale {
    fun current(): AppLanguage = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    fun apply(language: AppLanguage) =
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
