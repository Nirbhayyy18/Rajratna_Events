package com.rajratna.events.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ═══════════════════════════════════════════════════════════
// Theme Mode Enum
// ═══════════════════════════════════════════════════════════

/**
 * Three-state theme selection.
 * - LIGHT  : Always light
 * - DARK   : Always dark
 * - SYSTEM : Follow device appearance preference (default)
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

// ═══════════════════════════════════════════════════════════
// DataStore — Theme Preferences
// ═══════════════════════════════════════════════════════════

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

class ThemePreferences(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    /** Observe the currently persisted [ThemeMode]. Defaults to [ThemeMode.SYSTEM]. */
    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    /** Persist a new [ThemeMode] choice. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }
}
