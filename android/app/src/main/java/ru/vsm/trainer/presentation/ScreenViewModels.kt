package ru.vsm.trainer.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.core.userMessage
import ru.vsm.trainer.data.local.ActiveRun
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.AppNotification
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.remote.dto.Leaderboard
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardRow
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.data.remote.dto.NotificationList
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.remote.dto.Recommendation
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.repository.ActiveRunStore
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository

/** Загрузка с сохранением прежних данных: при ошибке обновления экран не пустеет. */
private suspend fun <T> MutableStateFlow<ScreenState<T>>.load(fetch: suspend () -> ru.vsm.trainer.core.Loaded<T>) {
    try {
        val loaded = fetch()
        value = ScreenState.Content(loaded.value, loaded.staleSince)
    } catch (error: ApiError) {
        if (value !is ScreenState.Content) value = ScreenState.Failed(error.userMessage)
    }
}

data class HomeContent(
    val profile: Profile,
    val recommendation: Recommendation?,
    val scenarios: List<ScenarioSummary>,
    val myRank: LeaderboardRow?,
    val rankTitle: String?,
    val unread: Int,
    val activeRun: ActiveRun?,
)

/** Главная: кто я, уровень и XP, что пройти дальше, место в бригаде, непрочитанные уведомления. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrainerRepository,
    private val runs: RunRepository,
    private val activeRuns: ActiveRunStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<HomeContent>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<HomeContent>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.load { coroutineScope {
                val profile = async { repository.profile() }
                val scenarios = async { repository.scenarios() }
                val analytics = async { runCatching { repository.analytics() }.getOrNull() }
                val board = async { runCatching { repository.leaderboard(LeaderboardScope.BRIGADE, LeaderboardPeriod.WEEK) }.getOrNull() }
                val notifications = async { runCatching { repository.notifications() }.getOrNull() }
                val loadedProfile = profile.await()
                val loadedScenarios = scenarios.await()
                ru.vsm.trainer.core.Loaded(
                    HomeContent(
                        profile = loadedProfile.value,
                        recommendation = analytics.await()?.value?.recommendation,
                        scenarios = loadedScenarios.value,
                        myRank = board.await()?.value?.me,
                        rankTitle = board.await()?.value?.title,
                        unread = notifications.await()?.value?.unread ?: 0,
                        activeRun = validActiveRun(),
                    ),
                    loadedProfile.staleSince ?: loadedScenarios.staleSince,
                )
            } }
        }
    }

    /** Незавершённое прохождение могли закончить на другом устройстве или по таймеру — сверяемся с сервером. */
    private suspend fun validActiveRun(): ActiveRun? {
        val active = activeRuns.current ?: return null
        return try {
            if (runs.state(active.runId).status == RunStatus.IN_PROGRESS) active else null.also { activeRuns.clear() }
        } catch (offline: ApiError.Offline) {
            active
        } catch (error: ApiError) {
            activeRuns.clear()
            null
        }
    }
}

@HiltViewModel
class ScenarioListViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<ScenarioSummary>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<ScenarioSummary>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { _state.load { repository.scenarios() } }
    }
}

/** Разбор после сценария: последствия каждого решения, лучший вариант, стандарт по ситуации. */
@HiltViewModel
class DebriefViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val runs: RunRepository,
    private val repository: TrainerRepository,
) : ViewModel() {
    private val runId: String = checkNotNull(savedState["runId"])
    private val _state = MutableStateFlow<ScreenState<Debrief>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Debrief>> = _state.asStateFlow()
    var competenceTitles: Map<String, String> = emptyMap()
        private set

    init {
        load()
        viewModelScope.launch { repository.recordEvent("debrief_opened", runId = runId) }
    }

    fun load() {
        _state.value = ScreenState.Loading
        viewModelScope.launch {
            competenceTitles = runCatching { repository.meta().value.competences }.getOrDefault(competenceTitles)
            _state.value = try {
                ScreenState.Content(runs.debrief(runId))
            } catch (error: ApiError) {
                ScreenState.Failed(error.userMessage)
            }
        }
    }
}

data class ProfileContent(val profile: Profile, val competences: List<Pair<String, Int>>)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<ProfileContent>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<ProfileContent>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.load {
                val titles = runCatching { repository.meta().value.competences }.getOrDefault(emptyMap())
                val loaded = repository.profile()
                val points = loaded.value.competencePoints
                // Сильные компетенции сверху, проседающие снизу
                val competences = (points.keys + titles.keys).distinct()
                    .map { (titles[it] ?: it) to (points[it] ?: 0) }
                    .sortedByDescending { it.second }
                ru.vsm.trainer.core.Loaded(ProfileContent(loaded.value, competences), loaded.staleSince)
            }
        }
    }
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Leaderboard>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Leaderboard>> = _state.asStateFlow()
    val scope = MutableStateFlow(LeaderboardScope.BRIGADE)
    val period = MutableStateFlow(LeaderboardPeriod.WEEK)

    init {
        load()
    }

    fun select(scope: LeaderboardScope = this.scope.value, period: LeaderboardPeriod = this.period.value) {
        this.scope.value = scope
        this.period.value = period
        _state.value = ScreenState.Loading
        load()
    }

    fun load() {
        viewModelScope.launch { _state.load { repository.leaderboard(scope.value, period.value) } }
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<NotificationList>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<NotificationList>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { _state.load { repository.notifications() } }
    }

    fun open(notification: AppNotification) {
        viewModelScope.launch {
            repository.recordEvent("notification_opened", notificationId = notification.id)
            if (!notification.read) {
                runCatching { repository.markRead(notification.id) }
                _state.load { repository.notifications() }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { repository.markAllRead() }
            _state.load { repository.notifications() }
        }
    }
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val repository: TrainerRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Analytics>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Analytics>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { _state.load { repository.analytics() } }
    }
}
