package ru.vsm.trainer.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.userMessage
import ru.vsm.trainer.data.remote.SessionEvents
import ru.vsm.trainer.data.repository.AuthRepository
import ru.vsm.trainer.data.repository.TrainerRepository

data class LoginUiState(
    val signedIn: Boolean = false,
    val fullName: String? = null,
    val personnelNumber: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = !submitting && personnelNumber.isNotBlank() && password.isNotEmpty()
}

/** Вход, выход и истечение сессии. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val trainer: TrainerRepository,
    events: SessionEvents,
) : ViewModel() {
    private val _state = MutableStateFlow(auth.current.let { LoginUiState(signedIn = it != null, fullName = it?.fullName) })
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            events.sessionExpired.collect {
                forget()
                _state.update { it.copy(error = ApiError.SessionExpired.message) }
            }
        }
    }

    fun onNumberChange(value: String) = _state.update { it.copy(personnelNumber = value.filter(Char::isDigit), error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val session = auth.login(current.personnelNumber.trim(), current.password)
                // Пароль не остаётся в памяти экрана
                _state.update { it.copy(signedIn = true, fullName = session.fullName, password = "", submitting = false) }
            } catch (error: ApiError) {
                _state.update { it.copy(submitting = false, error = error.userMessage) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.logout()
            forget()
        }
    }

    /** Данные прежнего сотрудника на устройстве не оставляем. */
    private fun forget() {
        trainer.clearCache()
        _state.update { LoginUiState(personnelNumber = it.personnelNumber) }
    }
}
