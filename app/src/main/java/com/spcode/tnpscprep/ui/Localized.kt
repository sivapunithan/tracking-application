package com.spcode.tnpscprep.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Shows the Tamil name when the UI language is Tamil (and a Tamil name exists), else English. */
@Composable
fun localized(en: String, ta: String): String {
    val lang = LocalConfiguration.current.locales[0].language
    return if (lang == "ta" && ta.isNotBlank()) ta else en
}
