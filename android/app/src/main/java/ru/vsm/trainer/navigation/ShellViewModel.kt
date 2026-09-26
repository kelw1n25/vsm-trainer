package ru.vsm.trainer.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.vsm.trainer.data.local.AppPreferences
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.data.remote.dto.Role
import ru.vsm.trainer.data.repository.AuthRepository
import ru.vsm.trainer.data.repository.TrainerRepository

/** Шапка: кто вошёл, счётчик непрочитанных (опрос раз в 30 с, как на сайте), ползунок темы. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    auth: AuthRepository,
    private val repository: TrainerRepository,
    val preferences: AppPreferences,
) : ViewModel() {
    val fullName: String = auth.current?.fullName.orEmpty()
    val instructor: Boolean = auth.current?.role == Role.INSTRUCTOR
    private val _unread = MutableStateFlow(0)
    val unread = _unread.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshUnread()
                delay(POLL_MS)
            }
        }
    }

    /** Ошибку опроса не показываем: счётчик обновится при следующей попытке. */
    fun refreshUnread() {
        viewModelScope.launch { runCatching { _unread.value = repository.notifications().value.unread } }
    }

    fun toggleTheme() {
        preferences.setTheme(if (preferences.theme.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    private companion object {
        const val POLL_MS = 30_000L
    }
}
