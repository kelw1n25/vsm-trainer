package ru.vsm.trainer.core

import java.time.Instant

/** Данные экрана и их свежесть: `staleSince` задан, если сети нет и показан сохранённый ответ. */
data class Loaded<T>(val value: T, val staleSince: Instant? = null)

/** Состояние экрана с данными. */
sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data class Content<T>(val value: T, val staleSince: Instant? = null) : ScreenState<T>
    data class Failed(val message: String) : ScreenState<Nothing>
}

val Throwable.userMessage: String get() = (this as? ApiError ?: toApiError()).message ?: "Что-то пошло не так"

/** Загрузка экрана: без сети — сохранённые данные, при ошибке обновления уже показанное не пропадает. */
suspend fun <T> kotlinx.coroutines.flow.MutableStateFlow<ScreenState<T>>.loadFrom(fetch: suspend () -> Loaded<T>) {
    try {
        val loaded = fetch()
        value = ScreenState.Content(loaded.value, loaded.staleSince)
    } catch (error: ApiError) {
        if (value !is ScreenState.Content) value = ScreenState.Failed(error.message ?: "Не удалось загрузить")
    }
}

/** «Смирнов Алексей Андреевич» → «Алексей», как `firstName` сайта. */
fun firstName(fullName: String): String = fullName.split(" ").getOrNull(1) ?: fullName

/** Склонение по числу: plural(3, "финал", "финала", "финалов") → «финала». */
fun plural(count: Int, one: String, few: String, many: String): String {
    val tens = count % 100
    val units = count % 10
    return when {
        tens in 11..14 -> many
        units == 1 -> one
        units in 2..4 -> few
        else -> many
    }
}
