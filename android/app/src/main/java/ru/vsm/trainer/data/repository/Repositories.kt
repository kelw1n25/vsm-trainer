package ru.vsm.trainer.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.Loaded
import ru.vsm.trainer.core.apiCall
import ru.vsm.trainer.data.local.KeyValueStore
import ru.vsm.trainer.data.local.ResponseCache
import ru.vsm.trainer.data.remote.TrainerApi
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.Avatar
import ru.vsm.trainer.data.remote.dto.ChoiceRequest
import ru.vsm.trainer.data.remote.dto.ClientEvent
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.remote.dto.Leaderboard
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.data.remote.dto.LoginRequest
import ru.vsm.trainer.data.remote.dto.Meta
import ru.vsm.trainer.data.remote.dto.NodeRequest
import ru.vsm.trainer.data.remote.dto.NotificationList
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.remote.dto.RefreshRequest
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.remote.dto.SessionTokens
import ru.vsm.trainer.data.remote.dto.StartRunRequest
import ru.vsm.trainer.data.remote.dto.StoryMap
import ru.vsm.trainer.data.remote.dto.TeamMember
import ru.vsm.trainer.data.remote.dto.Handbook
import ru.vsm.trainer.security.TokenStore

/**
 * Данные экранов. Сначала сеть; без сети — последний сохранённый ответ (Remote API → Repository → Local Cache → UI).
 * Ошибку сервера (4xx/5xx) кэшем не маскируем: она означает, что данные неверны, а не что их нет.
 */
interface TrainerRepository {
    suspend fun profile(): Loaded<Profile>
    /** Аватар вошедшего сотрудника — один на все экраны: обновляется с профилем и после сохранения. */
    val myAvatar: StateFlow<Avatar>
    suspend fun updateAvatar(avatar: Avatar): Avatar
    suspend fun scenarios(): Loaded<List<ScenarioSummary>>
    suspend fun meta(): Loaded<Meta>
    suspend fun leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod): Loaded<Leaderboard>
    suspend fun notifications(): Loaded<NotificationList>
    suspend fun markRead(id: Int)
    suspend fun markAllRead()
    suspend fun analytics(): Loaded<Analytics>
    suspend fun employeeAnalytics(employeeId: Int): Loaded<Analytics>
    suspend fun team(): Loaded<List<TeamMember>>
    suspend fun storyMap(scenarioId: String): Loaded<StoryMap>
    suspend fun handbook(): Loaded<Handbook>
    suspend fun recordEvent(type: String, runId: String? = null, notificationId: Int? = null)
    fun clearCache()
}

class RemoteTrainerRepository(
    private val api: TrainerApi,
    private val cache: ResponseCache,
    private val json: Json,
) : TrainerRepository {

    private suspend fun <T> cached(key: String, serializer: KSerializer<T>, fetch: suspend () -> T): Loaded<T> = try {
        val value = apiCall(fetch)
        cache.write(key, json.encodeToString(serializer, value))
        Loaded(value)
    } catch (offline: ApiError.Offline) {
        val (saved, savedAt) = cache.read(key) ?: throw offline
        val value = runCatching { json.decodeFromString(serializer, saved) }.getOrElse { throw offline }
        Loaded(value, staleSince = savedAt)
    }

    private val _myAvatar = MutableStateFlow(Avatar())
    override val myAvatar: StateFlow<Avatar> = _myAvatar.asStateFlow()

    override suspend fun profile() = cached("profile", Profile.serializer()) { api.profile() }.also { _myAvatar.value = it.value.avatar }

    override suspend fun updateAvatar(avatar: Avatar): Avatar {
        val previous = _myAvatar.value
        // Выбор виден сразу; сервер отказал или нет сети — возвращаем прежний
        _myAvatar.value = avatar
        return try {
            apiCall { api.updateAvatar(avatar) }.also { _myAvatar.value = it }
        } catch (error: ApiError) {
            _myAvatar.value = previous
            throw error
        }
    }
    override suspend fun scenarios() = cached("scenarios", ListSerializer(ScenarioSummary.serializer())) { api.scenarios() }
    override suspend fun meta() = cached("meta", Meta.serializer()) { api.meta() }
    override suspend fun leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod) =
        cached("leaderboard-${scope.name}-${period.name}", Leaderboard.serializer()) {
            api.leaderboard(scope.name.lowercase(), period.name.lowercase())
        }
    override suspend fun notifications() = cached("notifications", NotificationList.serializer()) { api.notifications() }
    override suspend fun markRead(id: Int) { apiCall { api.markRead(id) } }
    override suspend fun markAllRead() { apiCall { api.markAllRead() } }
    override suspend fun analytics() = cached("analytics", Analytics.serializer()) { api.analytics() }
    override suspend fun employeeAnalytics(employeeId: Int) =
        cached("analytics-$employeeId", Analytics.serializer()) { api.employeeAnalytics(employeeId) }
    override suspend fun team() = cached("team", ListSerializer(TeamMember.serializer())) { api.team() }
    override suspend fun storyMap(scenarioId: String) = cached("story-map-$scenarioId", StoryMap.serializer()) { api.storyMap(scenarioId) }
    override suspend fun handbook() = cached("handbook", Handbook.serializer()) { api.handbook() }

    /** Аналитическое событие не должно мешать пользователю: ошибку отправки не показываем. */
    override suspend fun recordEvent(type: String, runId: String?, notificationId: Int?) {
        runCatching { apiCall { api.recordEvent(ClientEvent(type, runId, notificationId, platform = "android")) } }
    }

    override fun clearCache() {
        cache.clear()
        _myAvatar.value = Avatar()
    }
}

/**
 * Прохождение сценария. Кэша нет намеренно: каждый шаг решает сервер по своему таймеру,
 * поэтому ответ, «сохранённый на потом», был бы недействительным.
 */
interface RunRepository {
    suspend fun start(scenarioId: String): RunState
    suspend fun state(runId: String): RunState
    suspend fun reveal(runId: String, nodeId: String): RunState
    suspend fun choose(runId: String, nodeId: String, choiceId: String): RunState
    suspend fun debrief(runId: String): Debrief
}

class RemoteRunRepository(private val api: TrainerApi) : RunRepository {
    override suspend fun start(scenarioId: String) = apiCall { api.startRun(StartRunRequest(scenarioId)) }
    override suspend fun state(runId: String) = apiCall { api.run(runId) }
    override suspend fun reveal(runId: String, nodeId: String) = apiCall { api.reveal(runId, NodeRequest(nodeId)) }
    override suspend fun choose(runId: String, nodeId: String, choiceId: String) =
        apiCall { api.choose(runId, ChoiceRequest(nodeId, choiceId)) }
    override suspend fun debrief(runId: String) = apiCall { api.debrief(runId) }
}

/** Вход и выход. */
class AuthRepository(private val api: TrainerApi, private val tokens: TokenStore) {
    val current: SessionTokens? get() = tokens.load()

    suspend fun login(personnelNumber: String, password: String): SessionTokens {
        val session = apiCall { api.login(LoginRequest(personnelNumber, password)) }
        tokens.save(session)
        return session
    }

    /** Отзываем refresh-токен на сервере; локально сессия удаляется даже без сети. */
    suspend fun logout() {
        tokens.load()?.let { runCatching { apiCall { api.logout(RefreshRequest(it.refreshToken)) } } }
        tokens.clear()
    }
}
