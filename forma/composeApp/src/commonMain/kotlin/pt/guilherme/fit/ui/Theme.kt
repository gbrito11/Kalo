package pt.guilherme.fit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FormaColors {
    val green = Color(0xFF2E7D5B)
    val greenLight = Color(0xFF7FD1A8)
    val protein = Color(0xFFE0675A)
    val carbs = Color(0xFFE6A23C)
    val fat = Color(0xFF5B8DEF)
    val water = Color(0xFF3BA7D9)
    val workout = Color(0xFF7C5CD6)
}

private val Light = lightColorScheme(
    primary = FormaColors.green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEDC),
    onPrimaryContainer = Color(0xFF0B2E1F),
    secondary = Color(0xFF4F6358),
    secondaryContainer = Color(0xFFD2E8DA),
    tertiary = FormaColors.workout,
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFE3EBE6),
)

private val Dark = darkColorScheme(
    primary = FormaColors.greenLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF15533A),
    onPrimaryContainer = Color(0xFFCDEEDC),
    secondary = Color(0xFFB6CCBE),
    secondaryContainer = Color(0xFF384B40),
    tertiary = Color(0xFFC8B6FF),
    background = Color(0xFF101412),
    surface = Color(0xFF101412),
    surfaceVariant = Color(0xFF2B332E),
)

@Composable
fun FormaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
