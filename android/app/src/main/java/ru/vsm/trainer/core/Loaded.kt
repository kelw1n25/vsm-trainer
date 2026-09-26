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
