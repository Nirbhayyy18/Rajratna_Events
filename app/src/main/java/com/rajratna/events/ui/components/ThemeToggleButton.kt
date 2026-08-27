package com.rajratna.events.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rajratna.events.ui.theme.ThemeMode

/**
 * A compact icon button in the TopAppBar that cycles through:
 *   SYSTEM (auto) → LIGHT (sun) → DARK (moon) → SYSTEM …
 *
 * Touch target is the standard 48×48dp IconButton.
 * Icon crossfades smoothly between states (200ms).
 *
 * Accessibility: contentDescription announces the NEXT state the button will switch to,
 * matching standard Android/iOS convention for toggle controls.
 */
@Composable
fun ThemeToggleButton(
    currentMode: ThemeMode,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, description) = when (currentMode) {
        ThemeMode.SYSTEM -> Triple(
            Icons.Outlined.BrightnessAuto,
            "Theme: Auto (system). Tap to switch to Light mode.",
            "Auto"
        )
        ThemeMode.LIGHT  -> Triple(
            Icons.Outlined.LightMode,
            "Theme: Light. Tap to switch to Dark mode.",
            "Light"
        )
        ThemeMode.DARK   -> Triple(
            Icons.Outlined.DarkMode,
            "Theme: Dark. Tap to switch to Auto (system) mode.",
            "Dark"
        )
    }

    IconButton(
        onClick = onCycleTheme,
        modifier = modifier.semantics {
            contentDescription = description.second
        }
    ) {
        Crossfade(
            targetState = currentMode,
            animationSpec = tween(durationMillis = 200),
            label = "theme_icon_crossfade"
        ) { mode ->
            when (mode) {
                ThemeMode.SYSTEM -> Icon(
                    imageVector = Icons.Outlined.BrightnessAuto,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemeMode.LIGHT  -> Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemeMode.DARK   -> Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
