package ru.vsm.trainer.data.remote

import java.io.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.vsm.trainer.data.remote.dto.RefreshRequest
import ru.vsm.trainer.security.TokenStore
import java.util.UUID

/** Подставляет access-токен и сквозной X-Request-ID, по которому поддержка найдёт запрос в журнале сервера. */
class AuthInterceptor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().header("X-Request-ID", UUID.randomUUID().toString())
        tokens.load()?.let { request.header("Authorization", "Bearer ${it.accessToken}") }
        return chain.proceed(request.build())
    }
}

/** Сессию восстановить нельзя — приложение показывает вход. */
class SessionEvents {
    private val expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = expired

    fun expire() {
        expired.tryEmit(Unit)
    }
}

/**
 * На 401 `invalid_token` обменивает refresh-токен и повторяет запрос. Обмен синхронизирован:
 * если на 401 наткнулись несколько запросов, обмен выполнит первый, остальные возьмут его результат.
 * Иначе второй запрос предъявил бы уже обменянный токен, и сервер закрыл бы все сессии как при краже.
 */
class TokenAuthenticator(
    private val tokens: TokenStore,
    private val authApi: () -> AuthApi,
    private val events: SessionEvents,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (!response.peekBody(4096).string().contains("\"invalid_token\"")) return null
        // Один повтор на запрос: если и новый токен отклонён, дальше не пробуем
        if (response.priorResponse != null) return null

        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        synchronized(lock) {
            val current = tokens.load() ?: return expire()
            if (current.accessToken != failed) return retry(response.request, current.accessToken)
            val renewed = try {
                authApi().refresh(RefreshRequest(current.refreshToken)).execute()
            } catch (error: IOException) {
                // Плохая связь — не повод выходить из аккаунта: пусть запрос завершится ошибкой сети
                throw error
            }
            val body = renewed.body()
            // Отказ в обмене — сессии больше нет. Сбой сервера (5xx) сессию не сбрасывает
            if (renewed.code() == 401) return expire()
            if (!renewed.isSuccessful || body == null) return null
            tokens.save(body)
            return retry(response.request, body.accessToken)
        }
    }

    private fun retry(request: Request, accessToken: String) =
        request.newBuilder().header("Authorization", "Bearer $accessToken").build()

    private fun expire(): Request? {
        tokens.clear()
        events.expire()
        return null
    }
}
