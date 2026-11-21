package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val BodyFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible, FontWeight.Normal)
)

// Generous vertical metrics + includeFontPadding=true so English, Bangla (বাংলা), and Hindi (हिन्दी)
// tall diacritics & conjuncts NEVER clip, collide, or overlap between lines or cards.
private val SafePlatformStyle = PlatformTextStyle(includeFontPadding = true)
private val SafeLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val SkipBuddyTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 46.sp,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        lineBreak = LineBreak.Heading,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 30.sp,
        lineBreak = LineBreak.Heading,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        lineBreak = LineBreak.Heading,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        lineBreak = LineBreak.Heading,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        lineBreak = LineBreak.Paragraph,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 27.sp,
        lineBreak = LineBreak.Paragraph,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        lineBreak = LineBreak.Paragraph,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        lineBreak = LineBreak.Simple,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    labelMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        lineBreak = LineBreak.Simple,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        platformStyle = SafePlatformStyle,
        lineHeightStyle = SafeLineHeightStyle
    )
)
