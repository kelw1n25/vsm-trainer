package ru.vsm.trainer.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.ServerClock
import ru.vsm.trainer.core.userMessage
import ru.vsm.trainer.data.local.ActiveRun
import ru.vsm.trainer.data.remote.dto.Choice
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.Step
import ru.vsm.trainer.data.repository.ActiveRunStore
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository

enum class Phase {
    LOADING,
    /** Сцена и диалог на экране, варианты скрыты, таймер ещё не идёт. */
    READING,
    /** Варианты показаны; если у шага есть таймер — идёт обратный отсчёт. */
    CHOOSING,
    /** Запрос в пути — повторные нажатия игнорируются. */
    SUBMITTING,
    /** Время вышло: ждём, пока сервер применит последствия промедления. */
    TIMED_OUT,
    FINISHED,
    FAILED,
}

data class PlayerUiState(
    val phase: Phase = Phase.LOADING,
    val run: RunState? = null,
    /** Реакция персонажей и изменение шкал на последнем шаге — последствия решения. */
    val lastSteps: List<Step> = emptyList(),
    /** Сообщение, которое не прерывает сценарий (обрыв связи, ответ после таймера). */
    val notice: String? = null,
    val error: String? = null,
)

/**
 * Прохождение сценария. Исход шага, таймер и шкалы считает сервер; здесь — порядок действий
 * и защита от некорректных: двойное нажатие, ответ после таймера, повтор после обрыва связи.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val runs: RunRepository,
    private val activeRuns: ActiveRunStore,
    private val events: TrainerRepository,
    private val clock: Clock,
) : ViewModel() {
    private val scenarioId: String = checkNotNull(savedState["scenarioId"])
    private val serverClock = ServerClock()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Старт или продолжение: сервер вернёт незавершённое прохождение этого сценария, если оно есть. */
    fun load() {
        _state.update { it.copy(phase = Phase.LOADING, error = null) }
        viewModelScope.launch {
            try {
                apply(runs.start(scenarioId))
            } catch (error: ApiError) {
                _state.update { it.copy(phase = Phase.FAILED, error = error.userMessage) }
            }
        }
    }

    /** Сколько секунд осталось на решение во времени сервера; `null` — у шага нет таймера или он не идёт. */
    fun remainingSeconds(): Double? {
        val current = _state.value
        if (current.phase != Phase.CHOOSING && current.phase != Phase.SUBMITTING) return null
        val deadline = current.run?.node?.deadlineAt ?: return null
        return serverClock.remainingSeconds(deadline, clock.instant())
    }

    /** Сцена дочитана — показать варианты. Таймер стартует на сервере в этот момент. */
    fun revealChoices() {
        val current = _state.value
        val run = current.run ?: return
        val node = run.node ?: return
        if (current.phase != Phase.READING) return
        _state.update { it.copy(phase = Phase.SUBMITTING) }
        viewModelScope.launch {
            try {
                apply(runs.reveal(run.id, node.id))
            } catch (error: ApiError) {
                recover(error, Phase.READING)
            }
        }
    }

    fun choose(choice: Choice) {
        val current = _state.value
        val run = current.run ?: return
        val node = run.node ?: return
        // Фаза меняется синхронно, до запроса: второе нажатие увидит SUBMITTING и ничего не отправит
        if (current.phase != Phase.CHOOSING || choice !in node.choices) return
        _state.update { it.copy(phase = Phase.SUBMITTING, notice = null) }
        viewModelScope.launch {
            try {
                apply(runs.choose(run.id, node.id, choice.id))
            } catch (error: ApiError) {
                recover(error, Phase.CHOOSING)
            }
        }
    }

    /**
     * Вызывается экраном несколько раз в секунду. Когда время вышло, спрашиваем сервер: он применит
     * таймаут (штраф шкал и ветку промедления) и вернёт следующую сцену.
     */
    fun tick() {
        val current = _state.value
        val run = current.run ?: return
        val node = run.node ?: return
        val timeoutAt = node.timeoutAt ?: return
        if (current.phase != Phase.CHOOSING || serverClock.serverNow(clock.instant()) < timeoutAt) return
        _state.update { it.copy(phase = Phase.TIMED_OUT) }
        viewModelScope.launch {
            try {
                val fresh = runs.state(run.id)
                if (fresh.node?.id == node.id && fresh.status == RunStatus.IN_PROGRESS) {
                    // Сервер ещё не считает время истёкшим (задержка сети) — спросим на следующем тике
                    serverClock.sync(fresh.serverTime, clock.instant())
                    _state.update { it.copy(phase = Phase.CHOOSING, run = fresh) }
                } else {
                    apply(fresh)
                }
            } catch (error: ApiError) {
                _state.update { it.copy(phase = Phase.CHOOSING, notice = error.userMessage) }
            }
        }
    }

    /** Пользователь закрыл сценарий до финала: прогресс на сервере сохранён, фиксируем выход для аналитики. */
    fun exit() {
        val run = _state.value.run ?: return
        if (run.status != RunStatus.IN_PROGRESS) return
        viewModelScope.launch { events.recordEvent("run_exited", runId = run.id) }
    }

    private fun apply(run: RunState) {
        serverClock.sync(run.serverTime, clock.instant())
        val phase = if (run.status == RunStatus.IN_PROGRESS) {
            activeRuns.save(ActiveRun(run.id, run.scenarioId, run.scenarioTitle))
            if (run.node?.choicesShown == true) Phase.CHOOSING else Phase.READING
        } else {
            activeRuns.clear()
            Phase.FINISHED
        }
        _state.update { it.copy(phase = phase, run = run, lastSteps = run.lastSteps.ifEmpty { it.lastSteps }) }
    }

    /**
     * Ответ не принят. Время вышло или шаг уже сменился (повтор после обрыва, второе устройство) —
     * перечитываем состояние с сервера. Нет сети — остаёмся на шаге, ответ можно отправить ещё раз.
     */
    private suspend fun recover(error: ApiError, fallback: Phase) {
        val run = _state.value.run ?: return
        if (error.code in setOf("time_expired", "stale_node", "choices_not_shown", "run_finished")) {
            try {
                apply(runs.state(run.id))
                if (error.code == "time_expired") _state.update { it.copy(notice = "Время на решение истекло — ответ не принят.") }
            } catch (again: ApiError) {
                _state.update { it.copy(phase = fallback, notice = again.userMessage) }
            }
        } else {
            _state.update { it.copy(phase = fallback, notice = error.userMessage) }
        }
    }
}
