package ru.vsm.trainer.core

import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Ошибка обращения к backend. Сервер всегда отвечает `{"detail": {"code", "message"}}`:
 * по `code` клиент решает, что делать, а `message` показывает человеку.
 */
sealed class ApiError(message: String) : Exception(message) {
    /** Нет сети, таймаут, обрыв — запрос можно повторить. */
    data object Offline : ApiError("Нет связи с сервером. Проверьте интернет и повторите.")

    /** Сервер ответил ошибкой со своим кодом. */
    data class Server(val status: Int, override val code: String, val serverMessage: String) :
        ApiError(if (status >= 500) "Сервер временно недоступен. $serverMessage" else serverMessage)

    /** Сессия закончилась и обновить её не удалось — нужен вход. */
    data object SessionExpired : ApiError("Сессия истекла, войдите заново.")

    /** Ответ не совпал с ожидаемой схемой. */
    data object InvalidResponse : ApiError("Не удалось прочитать ответ сервера. Обновите приложение.")

    /** Код ошибки сервера (`time_expired`, `stale_node`, …); у сетевых ошибок его нет. */
    open val code: String? get() = null
}

@Serializable
private data class ErrorEnvelope(val detail: Detail) {
    @Serializable
    data class Detail(val code: String, val message: String)
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Любое исключение сетевого вызова → понятная ошибка с кодом сервера. */
fun Throwable.toApiError(): ApiError = when (this) {
    is ApiError -> this
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val detail = body?.let { runCatching { errorJson.decodeFromString<ErrorEnvelope>(it).detail }.getOrNull() }
        ApiError.Server(code(), detail?.code ?: "http_${code()}", detail?.message ?: "Ошибка сервера ${code()}")
    }
    is IOException -> ApiError.Offline
    is SerializationException, is IllegalArgumentException -> ApiError.InvalidResponse
    else -> ApiError.InvalidResponse
}

/** Выполнить вызов API, превратив исключения в [ApiError]. */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (error: Throwable) {
    if (error is kotlinx.coroutines.CancellationException) throw error
    throw error.toApiError()
}
