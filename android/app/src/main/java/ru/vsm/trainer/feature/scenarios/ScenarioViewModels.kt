package ru.vsm.trainer.feature.scenarios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.Loaded
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.core.loadFrom
import ru.vsm.trainer.data.remote.dto.HistoryItem
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.remote.dto.StoryMap
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class ScenarioListViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<ScenarioSummary>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<ScenarioSummary>>> = _state.asStateFlow()
    val category = MutableStateFlow<String?>(null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { _state.loadFrom { repository.scenarios() } }
    }
}

/** Страница сценария: описание, что ждёт внутри, свои прошлые попытки — как `ScenarioPage`. */
data class ScenarioDetail(val scenario: ScenarioSummary?, val attempts: List<HistoryItem>)

@HiltViewModel
class ScenarioDetailViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: TrainerRepository) : ViewModel() {
    val scenarioId: String = checkNotNull(savedState["scenarioId"])
    private val _state = MutableStateFlow<ScreenState<ScenarioDetail>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<ScenarioDetail>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.loadFrom {
                val scenarios = repository.scenarios()
                val attempts = runCatching { repository.profile().value.history.filter { it.scenarioId == scenarioId } }.getOrDefault(emptyList())
                Loaded(ScenarioDetail(scenarios.value.firstOrNull { it.id == scenarioId }, attempts), scenarios.staleSince)
            }
        }
    }
}

/** Развитие истории: открытые развилки и финалы. */
data class StoryMapContent(val title: String, val map: StoryMap)

@HiltViewModel
class StoryMapViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: TrainerRepository) : ViewModel() {
    val scenarioId: String = checkNotNull(savedState["scenarioId"])
    private val _state = MutableStateFlow<ScreenState<StoryMapContent>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<StoryMapContent>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.loadFrom {
                val map = repository.storyMap(scenarioId)
                val title = runCatching { repository.scenarios().value.firstOrNull { it.id == scenarioId }?.title }.getOrNull() ?: "Сценарий"
                Loaded(StoryMapContent(title, map.value), map.staleSince)
            }
        }
    }
}
