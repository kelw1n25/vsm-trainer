package ru.vsm.trainer.feature.handbook

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
import ru.vsm.trainer.data.remote.dto.Handbook
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.repository.TrainerRepository

/** Справочник и в каких сценариях отрабатывается каждая ситуация. */
data class HandbookContent(val handbook: Handbook, val trainedIn: Map<Int, List<ScenarioSummary>>)

@HiltViewModel
class HandbookViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: TrainerRepository) : ViewModel() {
    /** Номер ситуации из ссылки «Ситуация N» со страницы сценария — к ней прокручиваем. */
    val focusSituation: Int? = savedState.get<String>("situation")?.toIntOrNull()
    private val _state = MutableStateFlow<ScreenState<HandbookContent>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<HandbookContent>> = _state.asStateFlow()
    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            _state.loadFrom {
                val handbook = repository.handbook()
                val scenarios = runCatching { repository.scenarios().value }.getOrDefault(emptyList())
                val trainedIn = mutableMapOf<Int, MutableList<ScenarioSummary>>()
                scenarios.forEach { scenario -> scenario.situations.forEach { trainedIn.getOrPut(it) { mutableListOf() } += scenario } }
                Loaded(HandbookContent(handbook.value, trainedIn), handbook.staleSince)
            }
        }
    }
}
