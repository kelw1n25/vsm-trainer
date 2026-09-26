package ru.vsm.trainer.feature.team

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
import ru.vsm.trainer.data.remote.dto.TeamMember
import ru.vsm.trainer.data.repository.TrainerRepository

/** Для инструктора: проводники его депо. */
@HiltViewModel
class TeamViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<TeamMember>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<TeamMember>>> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.loadFrom { repository.team() } }
    }
}
