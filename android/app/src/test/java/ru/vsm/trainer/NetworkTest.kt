package ru.vsm.trainer

import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.core.apiCall
import ru.vsm.trainer.data.local.InMemoryResponseCache
import ru.vsm.trainer.data.remote.NetworkFactory
import ru.vsm.trainer.data.remote.SessionEvents
import ru.vsm.trainer.data.repository.RemoteTrainerRepository
import ru.vsm.trainer.security.InMemoryTokenStore

class NetworkTest {
    /** Правило JUnit: сервер поднимается перед тестом и гасится после него. */
    @get:Rule
    val server = MockWebServer()
    private val tokens = InMemoryTokenStore(tokens(access = "old", refresh = "r1"))
    private val events = SessionEvents()
    private val api by lazy { NetworkFactory.create(server.url("/").toString(), testJson, tokens, events) }

    private fun respond(handler: (RecordedRequest) -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = handler(request)
        }
    }

    private suspend inline fun <reified T : ApiError> assertFails(noinline block: suspend () -> Unit): T {
        try {
            apiCall { block() }
        } catch (error: ApiError) {
            assertTrue("ожидалась ${T::class.simpleName}, получена $error", error is T)
            return error as T
        }
        fail("ожидалась ошибка")
        throw AssertionError()
    }

    @Test
    fun serverErrorKeepsCodeAndMessage() = runTest {
        respond { MockResponse().setResponseCode(409).setBody(errorBody("time_expired", "Время на решение истекло")) }
        val error = assertFails<ApiError.Server> { withContext(Dispatchers.IO) { api.run("1") } }
        assertEquals("time_expired", error.code)
        assertEquals("Время на решение истекло", error.message)
    }

    /** Клиент к адресу, где никто не слушает, — как пропавшая связь в тоннеле. */
    private val offlineApi by lazy {
        val port = ServerSocket(0).use { it.localPort }
        NetworkFactory.create("http://127.0.0.1:$port/", testJson, tokens, events)
    }

    @Test
    fun unreachableServerIsOffline() = runTest {
        assertFails<ApiError.Offline> { withContext(Dispatchers.IO) { offlineApi.profile() } }
    }

    @Test
    fun expiredTokenIsRefreshedOnceForConcurrentRequests() = runTest {
        val refreshes = AtomicInteger()
        respond { request ->
            when {
                request.path == "/api/auth/refresh" -> {
                    refreshes.incrementAndGet()
                    MockResponse().setBody(tokensJson("new", "r2"))
                }
                request.getHeader("Authorization") == "Bearer new" -> MockResponse().setBody(Fixture.text("profile"))
                // Держим ответы 401, чтобы все три запроса пришли к обмену одновременно
                else -> MockResponse().setResponseCode(401).setBody(errorBody("invalid_token")).setBodyDelay(100, TimeUnit.MILLISECONDS)
            }
        }
        withContext(Dispatchers.IO) { List(3) { async { api.profile() } }.awaitAll() }
        assertEquals("второй обмен предъявил бы уже использованный токен", 1, refreshes.get())
        assertEquals("r2", tokens.load()?.refreshToken)
    }

    @Test
    fun rejectedRefreshEndsSession() = runTest {
        respond { request ->
            if (request.path == "/api/auth/refresh") MockResponse().setResponseCode(401).setBody(errorBody("invalid_refresh_token"))
            else MockResponse().setResponseCode(401).setBody(errorBody("invalid_token"))
        }
        assertFails<ApiError.Server> { withContext(Dispatchers.IO) { api.profile() } }
        assertNull("токены удалены с устройства", tokens.load())
    }

    @Test
    fun serverFailureDuringRefreshKeepsSession() = runTest {
        respond { request ->
            if (request.path == "/api/auth/refresh") MockResponse().setResponseCode(503).setBody(errorBody("internal_error"))
            else MockResponse().setResponseCode(401).setBody(errorBody("invalid_token"))
        }
        assertFails<ApiError.Server> { withContext(Dispatchers.IO) { api.profile() } }
        assertNotNull("из-за сбоя сервера из аккаунта не выкидываем", tokens.load())
    }

    @Test
    fun requestsCarryTokenAndRequestId() = runTest {
        respond { MockResponse().setBody(Fixture.text("scenarios")) }
        withContext(Dispatchers.IO) { api.scenarios() }
        val request = server.takeRequest()
        assertEquals("Bearer old", request.getHeader("Authorization"))
        assertNotNull(request.getHeader("X-Request-ID"))
    }

    @Test
    fun offlineReturnsCachedResponseMarkedStale() = runTest {
        respond { MockResponse().setBody(Fixture.text("profile")) }
        val cache = InMemoryResponseCache()
        val fresh = withContext(Dispatchers.IO) { RemoteTrainerRepository(api, cache, testJson).profile() }
        assertNull(fresh.staleSince)

        // Связь пропала: тот же кэш, но сервер недоступен
        val saved = withContext(Dispatchers.IO) { RemoteTrainerRepository(offlineApi, cache, testJson).profile() }
        assertEquals(fresh.value, saved.value)
        assertNotNull(saved.staleSince)
    }

    @Test
    fun serverErrorIsNotHiddenByCache() = runTest {
        val failing = AtomicInteger()
        respond { if (failing.get() == 0) MockResponse().setBody(Fixture.text("analytics")) else MockResponse().setResponseCode(500).setBody(errorBody("internal_error")) }
        val repository = RemoteTrainerRepository(api, InMemoryResponseCache(), testJson)
        withContext(Dispatchers.IO) { repository.analytics() }
        failing.set(1)
        try {
            withContext(Dispatchers.IO) { repository.analytics() }
            fail("ошибку сервера нельзя подменять кэшем")
        } catch (error: ApiError.Server) {
            assertEquals("internal_error", error.code)
        }
    }

    @Test
    fun clientEventCarriesPlatform() = runTest {
        respond { MockResponse().setResponseCode(202).setBody("""{"status": "accepted"}""") }
        withContext(Dispatchers.IO) {
            RemoteTrainerRepository(api, InMemoryResponseCache(), testJson).recordEvent("run_exited", runId = "r1")
        }
        val body = server.takeRequest().body.readUtf8()
        assertTrue("сервер отклоняет событие без платформы: $body", "\"platform\":\"android\"" in body)
        assertTrue(body, "\"run_id\":\"r1\"" in body)
    }
}
