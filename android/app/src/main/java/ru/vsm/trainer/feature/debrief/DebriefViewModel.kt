package ru.vsm.trainer.feature.debrief

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class DebriefViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val runs: RunRepository,
    private val repository: TrainerRepository,
) : ViewModel() {
    private val runId: String = checkNotNull(savedState["runId"])
    private val _state = MutableStateFlow<ScreenState<Debrief>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Debrief>> = _state.asStateFlow()
    val titles = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        load()
        viewModelScope.launch { repository.recordEvent("debrief_opened", runId = runId) }
    }

    fun load() {
        viewModelScope.launch {
            runCatching { titles.value = repository.meta().value.competences }
            _state.value = try {
                ScreenState.Content(runs.debrief(runId))
            } catch (error: ApiError) {
                ScreenState.Failed(error.message ?: "Не удалось загрузить разбор")
            }
        }
    }
}
