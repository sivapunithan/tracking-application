package com.spcode.tnpscprep.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Shows the Tamil name when the UI language is Tamil (and a Tamil name exists), else English. */
@Composable
fun localized(en: String, ta: String): String {
    val lang = LocalConfiguration.current.locales[0].language
    return if (lang == "ta" && ta.isNotBlank()) ta else en
}

/** A name shown in a greeting: first letter upper-case ("k" -> "K"). Tamil has no case, so it is left as typed. */
fun properName(n: String): String = n.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
