package ru.vsm.trainer.feature.analytics

import androidx.lifecycle.SavedStateHandle
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
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.HistoryItem
import ru.vsm.trainer.data.repository.TrainerRepository

/** Аналитика своя или проводника для инструктора (`/team/:id` сайта). */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: TrainerRepository) : ViewModel() {
    private val employeeId: Int? = savedState.get<String>("employeeId")?.toIntOrNull()
    val own: Boolean get() = employeeId == null
    private val _state = MutableStateFlow<ScreenState<Analytics>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Analytics>> = _state.asStateFlow()
    val history = MutableStateFlow<List<HistoryItem>>(emptyList())

    fun load() {
        viewModelScope.launch {
            _state.loadFrom { employeeId?.let { repository.employeeAnalytics(it) } ?: repository.analytics() }
            // История доступна только по себе: профиль другого сотрудника инструктору не нужен
            if (own) runCatching { history.value = repository.profile().value.history }
        }
    }
}
