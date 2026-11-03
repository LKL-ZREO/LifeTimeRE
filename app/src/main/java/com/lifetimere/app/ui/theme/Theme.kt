package com.lifetimere.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PixelInk = Color(0xFF0A1117)
val PixelNight = Color(0xFF101B24)
val PixelPanel = Color(0xFF172831)
val PixelPanelRaised = Color(0xFF20383B)
val PixelOutline = Color(0xFF4C6465)
val PixelParchment = Color(0xFFF0E1BD)
val PixelMuted = Color(0xFFA8B5A9)
val PixelGold = Color(0xFFE3B35A)
val PixelAmber = Color(0xFFF0C96D)
val PixelForest = Color(0xFF4E8C67)
val PixelTeal = Color(0xFF5FA9A2)
val PixelCoral = Color(0xFFD16D5B)
val PixelViolet = Color(0xFF8078A8)

private val EchoColors = darkColorScheme(
    primary = PixelGold,
    onPrimary = Color(0xFF2A1D0E),
    primaryContainer = Color(0xFF4A371C),
    onPrimaryContainer = PixelParchment,
    secondary = PixelTeal,
    onSecondary = PixelInk,
    tertiary = PixelCoral,
    background = PixelNight,
    onBackground = PixelParchment,
    surface = PixelPanel,
    onSurface = PixelParchment,
    surfaceVariant = PixelPanelRaised,
    onSurfaceVariant = PixelMuted,
    outline = PixelOutline,
    error = PixelCoral,
)

private val PixelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun LifeTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EchoColors,
        typography = PixelTypography,
        shapes = Shapes(
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(3.dp),
            large = RoundedCornerShape(4.dp),
        ),
        content = content,
    )
}
