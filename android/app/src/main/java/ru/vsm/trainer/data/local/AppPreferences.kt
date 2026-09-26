package ru.vsm.trainer.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK }

/**
 * Настройки интерфейса, как на сайте: тема (ползунок в шапке и переключатель в настройках)
 * и «Уменьшить анимацию». Без сохранённого выбора тема следует системной.
 */
class AppPreferences(private val store: KeyValueStore, systemDark: Boolean) {
    private val _theme = MutableStateFlow(
        when (store.getString(THEME)) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
        },
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _reduceMotion = MutableStateFlow(store.getString(REDUCE_MOTION) == "1")
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        store.putString(THEME, mode.name.lowercase())
    }

    fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
        store.putString(REDUCE_MOTION, if (enabled) "1" else "0")
    }

    private companion object {
        const val THEME = "vsm_theme"
        const val REDUCE_MOTION = "vsm_reduce_motion"
    }
}
