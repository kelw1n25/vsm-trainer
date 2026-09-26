package ru.vsm.trainer.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.vsm.trainer.security.TokenStore

/** Сборка HTTP-клиента — одна и та же в приложении (Hilt) и в тестах (MockWebServer). */
object NetworkFactory {
    fun create(baseUrl: String, json: Json, tokens: TokenStore, events: SessionEvents): TrainerApi {
        val converter = json.asConverterFactory("application/json".toMediaType())
        // Отдельный клиент без Authenticator для обмена токена — иначе 401 на обмене вызвал бы обмен снова
        val authApi by lazy {
            Retrofit.Builder().baseUrl(baseUrl).client(client().build()).addConverterFactory(converter).build().create(AuthApi::class.java)
        }
        val client = client()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenAuthenticator(tokens, { authApi }, events))
            .build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(converter).build().create(TrainerApi::class.java)
    }

    // В вагоне связь пропадает: ждать ответа минуту нет смысла
    private fun client() = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
}
