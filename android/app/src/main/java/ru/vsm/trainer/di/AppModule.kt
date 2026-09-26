package ru.vsm.trainer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import ru.vsm.trainer.BuildConfig
import ru.vsm.trainer.data.local.FileResponseCache
import ru.vsm.trainer.data.local.KeyValueStore
import ru.vsm.trainer.data.local.PreferencesStore
import ru.vsm.trainer.data.remote.NetworkFactory
import ru.vsm.trainer.data.remote.SessionEvents
import ru.vsm.trainer.data.remote.TrainerApi
import ru.vsm.trainer.data.repository.ActiveRunStore
import ru.vsm.trainer.data.repository.AuthRepository
import ru.vsm.trainer.data.repository.RemoteRunRepository
import ru.vsm.trainer.data.repository.RemoteTrainerRepository
import ru.vsm.trainer.data.repository.RunRepository
import ru.vsm.trainer.data.repository.TrainerRepository
import ru.vsm.trainer.domain.NotificationSync
import ru.vsm.trainer.security.KeystoreTokenStore
import ru.vsm.trainer.security.TokenStore

/** Сборка зависимостей: адрес API, где токены и кэш, какие репозитории. ViewModel получают их через конструктор. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun clock(): Clock = Clock.systemUTC()

    @Provides @Singleton
    fun tokenStore(@ApplicationContext context: Context, json: Json): TokenStore = KeystoreTokenStore(context, json)

    @Provides @Singleton
    fun keyValueStore(@ApplicationContext context: Context): KeyValueStore =
        PreferencesStore(context.getSharedPreferences("settings", Context.MODE_PRIVATE))

    @Provides @Singleton
    fun sessionEvents() = SessionEvents()

    @Provides @Singleton
    fun trainerApi(json: Json, tokens: TokenStore, events: SessionEvents): TrainerApi =
        NetworkFactory.create(BuildConfig.API_BASE_URL, json, tokens, events)

    @Provides @Singleton
    fun trainerRepository(api: TrainerApi, @ApplicationContext context: Context, json: Json): TrainerRepository =
        RemoteTrainerRepository(api, FileResponseCache(File(context.cacheDir, "api-cache")), json)

    @Provides @Singleton
    fun runRepository(api: TrainerApi): RunRepository = RemoteRunRepository(api)

    @Provides @Singleton
    fun authRepository(api: TrainerApi, tokens: TokenStore) = AuthRepository(api, tokens)

    @Provides @Singleton
    fun activeRunStore(store: KeyValueStore, json: Json) = ActiveRunStore(store, json)

    @Provides @Singleton
    fun notificationSync(repository: TrainerRepository, store: KeyValueStore) = NotificationSync(repository, store)
}
