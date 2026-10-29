package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = Color(0xFF003732),
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFBAE6FD),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = EmergencyRed,
    errorContainer = Color(0xFF7F1D1D),
    onError = Color.White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryLight,
    onPrimaryContainer = TealPrimaryDark,
    secondary = ClinicalBlue,
    onSecondary = Color.White,
    secondaryContainer = ClinicalBlueContainer,
    onSecondaryContainer = ClinicalBlueOnContainer,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    error = EmergencyRed,
    errorContainer = EmergencyRedContainer,
    onError = Color.White,
    outline = SlateBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep dedicated medical branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
