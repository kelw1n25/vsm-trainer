package ru.vsm.trainer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.data.local.AppPreferences
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: TrainerRepository, val preferences: AppPreferences) : ViewModel() {
    val profile = MutableStateFlow<Profile?>(null)

    init {
        viewModelScope.launch { runCatching { profile.value = repository.profile().value } }
    }

    fun setDark(dark: Boolean) = preferences.setTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)

    fun setReduceMotion(enabled: Boolean) = preferences.setReduceMotion(enabled)
}
