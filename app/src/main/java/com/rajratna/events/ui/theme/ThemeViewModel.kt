package com.rajratna.events.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel that holds and persists the user-selected [ThemeMode].
 * Scoped to MainActivity so it survives recomposition without recreation.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = ThemePreferences(application)

    /** The currently selected theme mode, defaulting to [ThemeMode.SYSTEM]. */
    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM
    )

    /** Persist and apply a new theme selection. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    /** Cycle through SYSTEM → LIGHT → DARK → SYSTEM. */
    fun cycleTheme() {
        val next = when (themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT  -> ThemeMode.DARK
            ThemeMode.DARK   -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }
}
