package ru.vsm.trainer.data.remote.dto

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Модели ответов backend один к одному. Имена полей — из OpenAPI (docs/openapi.yaml).

@Serializable
enum class Role {
    @SerialName("conductor") CONDUCTOR,
    @SerialName("instructor") INSTRUCTOR,
}

@Serializable
data class LoginRequest(
    @SerialName("personnel_number") val personnelNumber: String,
    val password: String,
)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class SessionTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("employee_id") val employeeId: Int,
    @SerialName("full_name") val fullName: String,
    val role: Role,
)

@Serializable
data class ScenarioSummary(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: Int,
    @SerialName("service_class") val serviceClass: String,
    val route: String,
    val description: String,
    val situations: List<Int>,
    @SerialName("endings_total") val endingsTotal: Int,
)

@Serializable
data class Meta(val competences: Map<String, String>)

@Serializable
enum class RunStatus {
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("success") SUCCESS,
    @SerialName("partial") PARTIAL,
    @SerialName("failure") FAILURE,
}

@Serializable
data class Line(
    val speaker: String,
    val name: String? = null,
    val role: String? = null,
    val kind: String,
    val text: String,
)

@Serializable
data class Choice(val id: String, val text: String)

/** Текущий шаг. Пока сцена не дочитана (`choicesShown == false`), вариантов нет и таймер не идёт. */
@Serializable
data class Node(
    val id: String,
    val dialogue: List<Line>,
    @SerialName("choices_shown") val choicesShown: Boolean,
    val choices: List<Choice>,
    @SerialName("timer_seconds") val timerSeconds: Int? = null,
    @Serializable(InstantSerializer::class) @SerialName("deadline_at") val deadlineAt: Instant? = null,
    @Serializable(InstantSerializer::class) @SerialName("timeout_at") val timeoutAt: Instant? = null,
)

@Serializable
data class Step(
    val kind: String,
    val text: String,
    val reaction: List<Line>,
    @SerialName("loyalty_delta") val loyaltyDelta: Int,
    @SerialName("safety_delta") val safetyDelta: Int,
    val competences: Map<String, Int>,
) {
    val isTimeout: Boolean get() = kind == "timeout"
}

@Serializable
data class Final(
    val outcome: RunStatus,
    val reason: String,
    val ending: String,
    val text: String,
    val dialogue: List<Line>,
    @SerialName("xp_earned") val xpEarned: Int,
    @SerialName("competence_points") val competencePoints: Map<String, Int>,
)

@Serializable
data class Achievement(val code: String, val title: String, val description: String)

@Serializable
data class Level(
    val level: Int,
    val title: String,
    val xp: Int,
    @SerialName("level_xp") val levelXp: Int,
    @SerialName("next_level_xp") val nextLevelXp: Int? = null,
) {
    /** Доля пути до следующего уровня, 0…1; на последнем уровне — 1. */
    val progress: Float
        get() = nextLevelXp?.takeIf { it > levelXp }?.let { ((xp - levelXp).toFloat() / (it - levelXp)).coerceIn(0f, 1f) } ?: 1f
}

/** Состояние прохождения — единственный источник правды о сценарии. Клиент его только показывает. */
@Serializable
data class RunState(
    val id: String,
    @SerialName("scenario_id") val scenarioId: String,
    @SerialName("scenario_title") val scenarioTitle: String,
    val category: String,
    val status: RunStatus,
    val loyalty: Int,
    val safety: Int,
    @SerialName("steps_taken") val stepsTaken: Int,
    @SerialName("steps_left") val stepsLeft: Int,
    val node: Node? = null,
    val final: Final? = null,
    @SerialName("last_steps") val lastSteps: List<Step>,
    @SerialName("new_achievements") val newAchievements: List<Achievement>,
    @SerialName("level_up") val levelUp: Level? = null,
    @Serializable(InstantSerializer::class) @SerialName("server_time") val serverTime: Instant,
)

@Serializable
data class StartRunRequest(@SerialName("scenario_id") val scenarioId: String)

@Serializable
data class NodeRequest(@SerialName("node_id") val nodeId: String)

@Serializable
data class ChoiceRequest(@SerialName("node_id") val nodeId: String, @SerialName("choice_id") val choiceId: String)

@Serializable
data class DebriefStep(
    val kind: String,
    val situation: String,
    @SerialName("chosen_text") val chosenText: String? = null,
    val explanation: String? = null,
    @SerialName("was_best") val wasBest: Boolean,
    @SerialName("best_text") val bestText: String? = null,
    @SerialName("best_explanation") val bestExplanation: String? = null,
    @SerialName("loyalty_delta") val loyaltyDelta: Int,
    @SerialName("safety_delta") val safetyDelta: Int,
    @SerialName("loyalty_after") val loyaltyAfter: Int,
    @SerialName("safety_after") val safetyAfter: Int,
    val competences: Map<String, Int>,
    @SerialName("elapsed_seconds") val elapsedSeconds: Double? = null,
    @SerialName("timer_seconds") val timerSeconds: Int? = null,
)

@Serializable
data class Situation(val number: Int, val title: String, val reaction: String, val phrases: List<String>)

@Serializable
data class Debrief(
    @SerialName("run_id") val runId: String,
    @SerialName("scenario_title") val scenarioTitle: String,
    val outcome: RunStatus,
    val ending: String,
    @SerialName("final_text") val finalText: String,
    @SerialName("xp_earned") val xpEarned: Int,
    @SerialName("competence_points") val competencePoints: Map<String, Int>,
    @SerialName("initial_loyalty") val initialLoyalty: Int,
    @SerialName("initial_safety") val initialSafety: Int,
    val loyalty: Int,
    val safety: Int,
    val decisions: Int,
    @SerialName("best_decisions") val bestDecisions: Int,
    val timeouts: Int,
    @SerialName("average_reaction_seconds") val averageReactionSeconds: Double? = null,
    val steps: List<DebriefStep>,
    val situations: List<Situation>,
) {
    /** Шаги, где выбор был не лучшим или истёк таймер, — «что можно было сделать иначе». */
    val missedSteps: List<DebriefStep> get() = steps.filter { !it.wasBest && it.bestText != null }
}

@Serializable
data class ProfileAchievement(
    val code: String,
    val title: String,
    val description: String,
    @Serializable(InstantSerializer::class) @SerialName("earned_at") val earnedAt: Instant? = null,
)

@Serializable
data class HistoryItem(
    @SerialName("run_id") val runId: String,
    @SerialName("scenario_title") val scenarioTitle: String,
    val outcome: RunStatus,
    @SerialName("xp_earned") val xpEarned: Int,
    @Serializable(InstantSerializer::class) @SerialName("finished_at") val finishedAt: Instant,
)

@Serializable
data class Profile(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val role: Role,
    val brigade: String,
    val depot: String,
    val level: Level,
    @SerialName("runs_completed") val runsCompleted: Int,
    @SerialName("competence_points") val competencePoints: Map<String, Int>,
    val achievements: List<ProfileAchievement>,
    val history: List<HistoryItem>,
) {
    val earnedAchievements: List<ProfileAchievement>
        get() = achievements.filter { it.earnedAt != null }.sortedByDescending { it.earnedAt }
}

@Serializable
enum class LeaderboardScope(val title: String) {
    @SerialName("brigade") BRIGADE("Бригада"),
    @SerialName("depot") DEPOT("Депо"),
    @SerialName("company") COMPANY("Компания"),
}

@Serializable
enum class LeaderboardPeriod(val title: String) {
    @SerialName("week") WEEK("Неделя"),
    @SerialName("month") MONTH("Месяц"),
    @SerialName("all") ALL("Всё время"),
}

@Serializable
data class LeaderboardRow(
    val rank: Int,
    @SerialName("employee_id") val employeeId: Int,
    @SerialName("full_name") val fullName: String,
    val brigade: String,
    @SerialName("level_title") val levelTitle: String,
    val points: Int,
    @SerialName("is_me") val isMe: Boolean,
)

@Serializable
data class Leaderboard(
    val scope: LeaderboardScope,
    val period: LeaderboardPeriod,
    val title: String,
    val participants: Int,
    val rows: List<LeaderboardRow>,
) {
    val me: LeaderboardRow? get() = rows.firstOrNull { it.isMe }
}

@Serializable
data class AppNotification(
    val id: Int,
    val type: String,
    val title: String,
    val body: String,
    @Serializable(InstantSerializer::class) @SerialName("created_at") val createdAt: Instant,
    val read: Boolean,
)

@Serializable
data class NotificationList(val unread: Int, val items: List<AppNotification>)

@Serializable
data class WeekPoint(
    @Serializable(InstantSerializer::class) @SerialName("week_start") val weekStart: Instant,
    val xp: Int,
    val runs: Int,
)

@Serializable
data class CompetenceStat(val code: String, val title: String, val points: Int)

@Serializable
data class Recommendation(@SerialName("scenario_id") val scenarioId: String, val title: String, val reason: String)

@Serializable
data class Analytics(
    @SerialName("total_runs") val totalRuns: Int,
    @SerialName("unfinished_runs") val unfinishedRuns: Int,
    @SerialName("completion_rate") val completionRate: Double? = null,
    @SerialName("avg_decision_seconds") val avgDecisionSeconds: Double? = null,
    val progress: List<WeekPoint>,
    val competences: List<CompetenceStat>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val mistakes: List<String>,
    val recommendation: Recommendation? = null,
)

/** Действие на клиенте, которое сервер сам увидеть не может (`POST /api/analytics/events`). */
@Serializable
data class ClientEvent(
    val type: String,
    @SerialName("run_id") val runId: String? = null,
    @SerialName("notification_id") val notificationId: Int? = null,
    val platform: String = "android",
)

@Serializable
data class Status(val status: String)
