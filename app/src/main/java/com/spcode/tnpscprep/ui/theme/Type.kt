package com.spcode.tnpscprep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Tamil glyphs have tall ascenders/descenders and stacked marks; Material's default line heights
 * (about 1.2-1.5x) clip them. For Tamil we enforce a minimum ratio and split the extra space evenly
 * above and below the text (Trim.None), so nothing is cut at the top or bottom of a line.
 * The glyphs themselves come from the system's Noto Sans Tamil fallback: no font is bundled.
 */
private const val TAMIL_MIN_LINE_HEIGHT_RATIO = 1.6f

private fun TextStyle.forTamil(): TextStyle {
    if (!fontSize.isSp || !lineHeight.isSp) return this
    return copy(
        lineHeight = maxOf(lineHeight.value, fontSize.value * TAMIL_MIN_LINE_HEIGHT_RATIO).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
}

fun tnpscTypography(tamil: Boolean): Typography {
    val base = Typography()
    if (!tamil) return base
    return Typography(
        displayLarge = base.displayLarge.forTamil(), displayMedium = base.displayMedium.forTamil(),
        displaySmall = base.displaySmall.forTamil(),
        headlineLarge = base.headlineLarge.forTamil(), headlineMedium = base.headlineMedium.forTamil(),
        headlineSmall = base.headlineSmall.forTamil(),
        titleLarge = base.titleLarge.forTamil(), titleMedium = base.titleMedium.forTamil(),
        titleSmall = base.titleSmall.forTamil(),
        bodyLarge = base.bodyLarge.forTamil(), bodyMedium = base.bodyMedium.forTamil(),
        bodySmall = base.bodySmall.forTamil(),
        labelLarge = base.labelLarge.forTamil(), labelMedium = base.labelMedium.forTamil(),
        labelSmall = base.labelSmall.forTamil(),
    )
}
