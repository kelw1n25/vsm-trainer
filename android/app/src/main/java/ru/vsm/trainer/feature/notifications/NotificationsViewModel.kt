package ru.vsm.trainer.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.core.loadFrom
import ru.vsm.trainer.data.remote.dto.AppNotification
import ru.vsm.trainer.data.remote.dto.NotificationList
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<NotificationList>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<NotificationList>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { _state.loadFrom { repository.notifications() } }
    }

    /** Касание уведомления: отмечаем прочитанным и фиксируем открытие для аналитики. */
    fun open(notification: AppNotification) {
        viewModelScope.launch {
            repository.recordEvent("notification_opened", notificationId = notification.id)
            if (!notification.read) {
                runCatching { repository.markRead(notification.id) }
                _state.loadFrom { repository.notifications() }
            }
        }
    }

    fun readAll() {
        viewModelScope.launch {
            runCatching { repository.markAllRead() }
            _state.loadFrom { repository.notifications() }
        }
    }
}
