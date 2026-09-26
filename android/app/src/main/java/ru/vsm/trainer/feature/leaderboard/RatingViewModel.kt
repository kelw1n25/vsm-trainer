package ru.vsm.trainer.feature.leaderboard

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
import ru.vsm.trainer.data.remote.dto.Leaderboard
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.data.remote.dto.Level
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class RatingViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Leaderboard>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Leaderboard>> = _state.asStateFlow()
    val scope = MutableStateFlow(LeaderboardScope.BRIGADE)
    val period = MutableStateFlow(LeaderboardPeriod.WEEK)
    val level = MutableStateFlow<Level?>(null)

    init {
        viewModelScope.launch { runCatching { level.value = repository.profile().value.level } }
        load()
    }

    fun select(scope: LeaderboardScope = this.scope.value, period: LeaderboardPeriod = this.period.value) {
        this.scope.value = scope
        this.period.value = period
        load()
    }

    fun load() {
        viewModelScope.launch { _state.loadFrom { repository.leaderboard(scope.value, period.value) } }
    }
}
