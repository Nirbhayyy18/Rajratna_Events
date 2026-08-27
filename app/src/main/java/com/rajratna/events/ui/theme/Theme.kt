package com.rajratna.events.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════
// Light Color Scheme — Intentionally Designed
// ═══════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary             = Teal40,
    onPrimary           = Color.White,
    primaryContainer    = TealContainer,
    onPrimaryContainer  = OnTealContainer,
    secondary           = Orange40,
    onSecondary         = Color.White,
    secondaryContainer  = OrangeContainer,
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary            = Slate40,
    onTertiary          = Color.White,
    background          = LightBackground,
    onBackground        = LightOnBackground,
    surface             = LightSurface,
    onSurface           = LightOnSurface,
    surfaceVariant      = LightSurfaceVariant,
    onSurfaceVariant    = LightOnSurfaceVar,
    surfaceContainerHighest = LightSurfaceVariant,
    outline             = LightOutline,
    outlineVariant      = LightOutlineVariant,
    error               = Color(0xFFB3261E),
    onError             = Color.White,
    scrim               = Color(0xFF000000)
)

// ═══════════════════════════════════════════════════════════
// Dark Color Scheme — Intentionally Designed (NOT inverted)
// ═══════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary             = Teal80,
    onPrimary           = Color(0xFF003731),
    primaryContainer    = TealContainerDark,
    onPrimaryContainer  = OnTealContainerDark,
    secondary           = Orange80,
    onSecondary         = Color(0xFF3E1500),
    secondaryContainer  = OrangeContainerDark,
    onSecondaryContainer = Orange80,
    tertiary            = Slate80,
    onTertiary          = Color(0xFF192228),
    background          = DarkBackground,
    onBackground        = DarkOnBackground,
    surface             = DarkSurface,
    onSurface           = DarkOnSurface,
    surfaceVariant      = DarkSurfaceVariant,
    onSurfaceVariant    = DarkOnSurfaceVar,
    surfaceContainerHighest = DarkSurfaceElev,
    outline             = DarkOutline,
    outlineVariant      = DarkOutlineVariant,
    error               = Color(0xFFEF9A9A),
    onError             = Color(0xFF601410),
    scrim               = Color(0xFF000000)
)

// ═══════════════════════════════════════════════════════════
// Theme Composable
// ═══════════════════════════════════════════════════════════

@Composable
fun RajratnaEventsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Dynamic color intentionally disabled: ensures our designed palettes are always used.
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> systemDark
    }

    // Animate every color slot for a smooth, non-distracting theme transition (~280ms)
    val animSpec = tween<Color>(durationMillis = 280)
    val target = if (darkTheme) DarkColorScheme else LightColorScheme

    @Suppress("NAME_SHADOWING")
    val colorScheme = ColorScheme(
        primary                  = animateColorAsState(target.primary, animSpec).value,
        onPrimary                = animateColorAsState(target.onPrimary, animSpec).value,
        primaryContainer         = animateColorAsState(target.primaryContainer, animSpec).value,
        onPrimaryContainer       = animateColorAsState(target.onPrimaryContainer, animSpec).value,
        secondary                = animateColorAsState(target.secondary, animSpec).value,
        onSecondary              = animateColorAsState(target.onSecondary, animSpec).value,
        secondaryContainer       = animateColorAsState(target.secondaryContainer, animSpec).value,
        onSecondaryContainer     = animateColorAsState(target.onSecondaryContainer, animSpec).value,
        tertiary                 = animateColorAsState(target.tertiary, animSpec).value,
        onTertiary               = animateColorAsState(target.onTertiary, animSpec).value,
        tertiaryContainer        = animateColorAsState(target.tertiaryContainer, animSpec).value,
        onTertiaryContainer      = animateColorAsState(target.onTertiaryContainer, animSpec).value,
        background               = animateColorAsState(target.background, animSpec).value,
        onBackground             = animateColorAsState(target.onBackground, animSpec).value,
        surface                  = animateColorAsState(target.surface, animSpec).value,
        onSurface                = animateColorAsState(target.onSurface, animSpec).value,
        surfaceVariant           = animateColorAsState(target.surfaceVariant, animSpec).value,
        onSurfaceVariant         = animateColorAsState(target.onSurfaceVariant, animSpec).value,
        surfaceTint              = animateColorAsState(target.surfaceTint, animSpec).value,
        inverseSurface           = animateColorAsState(target.inverseSurface, animSpec).value,
        inverseOnSurface         = animateColorAsState(target.inverseOnSurface, animSpec).value,
        inversePrimary           = animateColorAsState(target.inversePrimary, animSpec).value,
        error                    = animateColorAsState(target.error, animSpec).value,
        onError                  = animateColorAsState(target.onError, animSpec).value,
        errorContainer           = animateColorAsState(target.errorContainer, animSpec).value,
        onErrorContainer         = animateColorAsState(target.onErrorContainer, animSpec).value,
        outline                  = animateColorAsState(target.outline, animSpec).value,
        outlineVariant           = animateColorAsState(target.outlineVariant, animSpec).value,
        scrim                    = animateColorAsState(target.scrim, animSpec).value,
        surfaceBright            = animateColorAsState(target.surfaceBright, animSpec).value,
        surfaceDim               = animateColorAsState(target.surfaceDim, animSpec).value,
        surfaceContainer         = animateColorAsState(target.surfaceContainer, animSpec).value,
        surfaceContainerHigh     = animateColorAsState(target.surfaceContainerHigh, animSpec).value,
        surfaceContainerHighest  = animateColorAsState(target.surfaceContainerHighest, animSpec).value,
        surfaceContainerLow      = animateColorAsState(target.surfaceContainerLow, animSpec).value,
        surfaceContainerLowest   = animateColorAsState(target.surfaceContainerLowest, animSpec).value,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
