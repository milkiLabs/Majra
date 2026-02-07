package com.milkilabs.majra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.milkilabs.majra.settings.TypographyScale

// Set of Material typography styles to start with
private val BaseTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/** Return a typography set scaled uniformly for accessibility preferences. */
fun typographyForScale(scale: TypographyScale): Typography {
    val factor = when (scale) {
        TypographyScale.Small -> 0.9f
        TypographyScale.Default -> 1.0f
        TypographyScale.Large -> 1.1f
    }
    return Typography(
        displayLarge = BaseTypography.displayLarge.scale(factor),
        displayMedium = BaseTypography.displayMedium.scale(factor),
        displaySmall = BaseTypography.displaySmall.scale(factor),
        headlineLarge = BaseTypography.headlineLarge.scale(factor),
        headlineMedium = BaseTypography.headlineMedium.scale(factor),
        headlineSmall = BaseTypography.headlineSmall.scale(factor),
        titleLarge = BaseTypography.titleLarge.scale(factor),
        titleMedium = BaseTypography.titleMedium.scale(factor),
        titleSmall = BaseTypography.titleSmall.scale(factor),
        bodyLarge = BaseTypography.bodyLarge.scale(factor),
        bodyMedium = BaseTypography.bodyMedium.scale(factor),
        bodySmall = BaseTypography.bodySmall.scale(factor),
        labelLarge = BaseTypography.labelLarge.scale(factor),
        labelMedium = BaseTypography.labelMedium.scale(factor),
        labelSmall = BaseTypography.labelSmall.scale(factor),
    )
}

private fun TextStyle.scale(factor: Float): TextStyle {
    return copy(
        fontSize = fontSize * factor,
        lineHeight = lineHeight * factor,
        letterSpacing = letterSpacing * factor,
    )
}