package org.secretarrow.noiddroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import org.secretarrow.noiddroid.R

/**
 * Brutalist minimal palette — pure black/white with a single accent.
 * Inspired by Linear, Vercel, and the Swiss design tradition.
 * Light theme: paper background, ink text. Dark theme inverts.
 */
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF0A0A0A),
        onPrimary = Color(0xFFFAFAFA),
        secondary = Color(0xFF555555),
        onSecondary = Color(0xFFFAFAFA),
        tertiary = Color(0xFF1A1A1A),
        onTertiary = Color(0xFFFAFAFA),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF0A0A0A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color(0xFF555555),
        outline = Color(0xFFE5E5E5),
        outlineVariant = Color(0xFFFAFAFA),
        error = Color(0xFF0A0A0A),
        onError = Color(0xFFFAFAFA),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFAFAFA),
        onPrimary = Color(0xFF0A0A0A),
        secondary = Color(0xFFAAAAAA),
        onSecondary = Color(0xFF0A0A0A),
        tertiary = Color(0xFFE5E5E5),
        onTertiary = Color(0xFF0A0A0A),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFAFAFA),
        surface = Color(0xFF0A0A0A),
        onSurface = Color(0xFFFAFAFA),
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFFAAAAAA),
        outline = Color(0xFF262626),
        outlineVariant = Color(0xFF1A1A1A),
        error = Color(0xFFFAFAFA),
        onError = Color(0xFF0A0A0A),
    )

private val fontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

// Inter Tight — Linear.app uses Inter Display, we ship the open-source cousin.
private val InterTight =
    FontFamily(
        Font(googleFont = GoogleFont("Inter Tight"), fontProvider = fontProvider),
    )

// JetBrains Mono for hash rate / address digits — tabular numerals.
private val JetBrainsMono =
    FontFamily(
        Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = fontProvider),
    )

private val BrutalistTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Black,
                fontSize = 96.sp,
                lineHeight = 100.sp,
                letterSpacing = (-2).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp,
                lineHeight = 60.sp,
                letterSpacing = (-1).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.25).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = InterTight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.75.sp,
            ),
    )

@Composable
fun NoiddroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = BrutalistTypography,
        content = content,
    )
}
