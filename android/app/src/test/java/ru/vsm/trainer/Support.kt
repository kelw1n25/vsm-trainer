package ru.vsm.trainer

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import ru.vsm.trainer.core.Loaded
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.remote.dto.Leaderboard
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.data.remote.dto.Meta
import ru.vsm.trainer.data.remote.dto.NotificationList
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.remote.dto.Role
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.remote.dto.SessionTokens
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository

val testJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Настоящие ответы backend, снятые с работающего сервера на синтетических данных. */
object Fixture {
    fun text(name: String): String = checkNotNull(javaClass.getResource("/fixtures/$name.json")).readText()
    inline fun <reified T> decode(name: String): T = testJson.decodeFromString(text(name))
}

fun tokens(access: String = "access-1", refresh: String = "refresh-1") =
    SessionTokens(access, refresh, 43200, 2, "Синтетический Проводник", Role.CONDUCTOR)

fun tokensJson(access: String, refresh: String) = testJson.encodeToString(SessionTokens.serializer(), tokens(access, refresh))

fun errorBody(code: String, message: String = "сообщение") = """{"detail": {"code": "$code", "message": "$message"}}"""

/** Прохождение по сценарию теста: каждое действие отдаёт следующее заданное состояние или ошибку. */
class FakeRunRepository(var startResult: Result<RunState>) : RunRepository {
    var revealResult: Result<RunState>? = null
    val chooseResults = ArrayDeque<Result<RunState>>()
    var stateResult: Result<RunState>? = null
    var chooseCalls = 0
    var stateCalls = 0

    override suspend fun start(scenarioId: String) = startResult.getOrThrow()
    override suspend fun state(runId: String): RunState {
        stateCalls++
        return stateResult!!.getOrThrow()
    }
    override suspend fun reveal(runId: String, nodeId: String) = revealResult!!.getOrThrow()
    override suspend fun choose(runId: String, nodeId: String, choiceId: String): RunState {
        chooseCalls++
        // Пауза даёт шанс второму нажатию прийти, пока первый запрос «в пути»
        delay(20)
        return chooseResults.removeFirst().getOrThrow()
    }
    override suspend fun debrief(runId: String): Debrief = Fixture.decode("debrief")
}

/** Репозиторий экранов в памяти: хватает для проверки событий аналитики и уведомлений. */
class FakeTrainerRepository : TrainerRepository {
    var notificationsResult: Loaded<NotificationList> = Loaded(Fixture.decode("notifications"))
    val events = mutableListOf<String>()
    var cacheCleared = false

    override suspend fun profile() = Loaded(Fixture.decode<Profile>("profile"))
    override suspend fun scenarios() = Loaded(Fixture.decode<List<ScenarioSummary>>("scenarios"))
    override suspend fun meta() = Loaded(Fixture.decode<Meta>("meta"))
    override suspend fun leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod) = Loaded(Fixture.decode<Leaderboard>("leaderboard"))
    override suspend fun notifications() = notificationsResult
    override suspend fun markRead(id: Int) {}
    override suspend fun markAllRead() {}
    override suspend fun analytics() = Loaded(Fixture.decode<Analytics>("analytics"))
    override suspend fun recordEvent(type: String, runId: String?, notificationId: Int?) {
        events += type
    }
    override fun clearCache() {
        cacheCleared = true
    }
}
