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
    private val auth: AuthRepository,
    private val repository: TrainerRepository,
    val preferences: AppPreferences,
) : ViewModel() {
    // ViewModel живёт дольше сессии (уровень Activity): после выхода и входа другим сотрудником
    // имя и роль читаются заново, а не остаются от прежнего
    val fullName: String get() = auth.current?.fullName.orEmpty()
    val instructor: Boolean get() = auth.current?.role == Role.INSTRUCTOR
    private val _unread = MutableStateFlow(0)
    val unread = _unread.asStateFlow()
    /** Аватар вошедшего сотрудника для шапки и всех экранов. */
    val avatar = repository.myAvatar
    /** Своё фото на аватаре (если сотрудник его поставил). */
    val photo = repository.myPhoto

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshUnread()
                delay(POLL_MS)
            }
        }
    }

    /** Профиль загружается при входе — аватар в шапке сразу нового сотрудника; без сети остаётся по умолчанию. */
    fun refreshProfile() {
        viewModelScope.launch { runCatching { repository.profile() } }
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
