package ru.vsm.trainer.feature.gameplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.di.ApplicationScope
import kotlinx.coroutines.flow.asStateFlow
import ru.vsm.trainer.core.firstName
import ru.vsm.trainer.data.local.AppPreferences
import ru.vsm.trainer.data.local.StoryMemory
import ru.vsm.trainer.data.repository.AuthRepository
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository
import ru.vsm.trainer.domain.story.StoryEngine

/** Экран новеллы: держит движок и звук, отмечает выход из незавершённого сценария. */
@HiltViewModel
class StoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    runs: RunRepository,
    memory: StoryMemory,
    auth: AuthRepository,
    preferences: AppPreferences,
    clock: Clock,
    val audio: StoryAudio,
    private val events: TrainerRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val employeeId = auth.current?.employeeId ?: 0
    val playerName: String = auth.current?.fullName?.let(::firstName) ?: "Проводник"
    private val memoryStore = memory

    val engine = StoryEngine(
        scenarioId = checkNotNull(savedState["scenarioId"]),
        employeeId = employeeId,
        playerName = playerName,
        runs = runs,
        memory = memory,
        scope = viewModelScope,
        clock = clock,
        reduceMotion = { preferences.reduceMotion.value },
        sounds = audio,
    )

    private val _muted = MutableStateFlow(memory.muted(employeeId))
    val muted = _muted.asStateFlow()

    /** «Открыто финалов: N из M» на финальном экране — из архива веток сервера. */
    private val _endings = MutableStateFlow<Pair<Int, Int>?>(null)
    val endings = _endings.asStateFlow()

    /** Названия компетенций для списка изменений на финале. */
    val titles = MutableStateFlow<Map<String, String>>(emptyMap())

    fun loadEndings(scenarioId: String) {
        viewModelScope.launch {
            runCatching { events.storyMap(scenarioId).value }.getOrNull()?.let { map ->
                _endings.value = map.endings.count { it.reached } to map.endings.size
            }
        }
    }

    init {
        audio.setMuted(_muted.value)
        engine.start()
        viewModelScope.launch { runCatching { titles.value = events.meta().value.competences } }
    }

    fun toggleSound() {
        val next = !_muted.value
        _muted.value = next
        memoryStore.saveMuted(employeeId, next)
        audio.setMuted(next)
    }

    /**
     * «← Выйти»: прогресс сохранён на сервере; выход до финала фиксируем для аналитики.
     * Событие отправляется в области приложения — экран закрывается сразу, запрос не отменяется вместе с ним.
     */
    fun exit() {
        val run = engine.view.value.run ?: return
        if (run.status == RunStatus.IN_PROGRESS) appScope.launch { events.recordEvent("run_exited", runId = run.id) }
    }

    override fun onCleared() {
        engine.dispose()
        audio.release()
    }
}
