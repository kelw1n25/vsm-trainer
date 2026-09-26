package ru.vsm.trainer.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.ChoiceRequest
import ru.vsm.trainer.data.remote.dto.ClientEvent
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.remote.dto.Leaderboard
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
import ru.vsm.trainer.data.remote.dto.Status

/** REST API тренажёра (docs/openapi.yaml). Тот же API, что у веб-клиента, — второго API нет. */
interface TrainerApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): SessionTokens

    @POST("api/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Status

    @GET("api/profile")
    suspend fun profile(): Profile

    @GET("api/scenarios")
    suspend fun scenarios(): List<ScenarioSummary>

    @GET("api/meta")
    suspend fun meta(): Meta

    @POST("api/runs")
    suspend fun startRun(@Body body: StartRunRequest): RunState

    @GET("api/runs/{id}")
    suspend fun run(@Path("id") runId: String): RunState

    @POST("api/runs/{id}/reveal")
    suspend fun reveal(@Path("id") runId: String, @Body body: NodeRequest): RunState

    @POST("api/runs/{id}/choices")
    suspend fun choose(@Path("id") runId: String, @Body body: ChoiceRequest): RunState

    @GET("api/runs/{id}/debrief")
    suspend fun debrief(@Path("id") runId: String): Debrief

    @GET("api/leaderboard")
    suspend fun leaderboard(@Query("scope") scope: String, @Query("period") period: String): Leaderboard

    @GET("api/notifications")
    suspend fun notifications(): NotificationList

    @POST("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Int): Status

    @POST("api/notifications/read-all")
    suspend fun markAllRead(): Status

    @GET("api/analytics/me")
    suspend fun analytics(): Analytics

    @POST("api/analytics/events")
    suspend fun recordEvent(@Body body: ClientEvent): Status
}

/** Обмен refresh-токена — отдельно, без авторизации, чтобы Authenticator не зациклился на себе. */
interface AuthApi {
    @POST("api/auth/refresh")
    fun refresh(@Body body: RefreshRequest): retrofit2.Call<SessionTokens>
}
