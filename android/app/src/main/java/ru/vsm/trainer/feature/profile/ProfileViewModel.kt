package ru.vsm.trainer.feature.profile

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
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Profile>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Profile>> = _state.asStateFlow()
    val titles = MutableStateFlow<Map<String, String>>(emptyMap())

    fun load() {
        viewModelScope.launch {
            runCatching { titles.value = repository.meta().value.competences }
            _state.loadFrom { repository.profile() }
        }
    }
}
