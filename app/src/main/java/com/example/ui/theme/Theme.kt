package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = OpticaTealPrimary,
    onPrimary = Color.White,
    primaryContainer = OpticaPrimaryContainerLight,
    onPrimaryContainer = OpticaOnPrimaryContainerLight,
    secondary = OpticaCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = OpticaSecondaryContainerLight,
    onSecondaryContainer = OpticaOnSecondaryContainerLight,
    tertiary = OpticaAccentOrange,
    onTertiary = Color.White,
    tertiaryContainer = OpticaTertiaryContainerLight,
    background = OpticaBackgroundLight,
    onBackground = OpticaTextDark,
    surface = OpticaSurfaceLight,
    onSurface = OpticaTextDark,
    surfaceVariant = OpticaSurfaceVariantLight,
    onSurfaceVariant = OpticaOnSurfaceVariantLight,
    outline = OpticaOutlineLight
  )

private val DarkColorScheme = LightColorScheme // Force bright, clean light aesthetic as requested

@Composable
fun OpticaCareTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

