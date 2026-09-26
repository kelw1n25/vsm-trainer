package ru.vsm.trainer.domain

import ru.vsm.trainer.data.local.KeyValueStore
import ru.vsm.trainer.data.remote.dto.AppNotification
import ru.vsm.trainer.data.repository.TrainerRepository

/**
 * Какие уведомления сервера ещё не показаны системным баннером. Запоминает наибольший показанный id,
 * чтобы одно и то же уведомление не всплывало при каждой фоновой синхронизации.
 */
class NotificationSync(private val repository: TrainerRepository, private val store: KeyValueStore) {
    suspend fun fresh(): List<AppNotification> {
        val loaded = repository.notifications()
        // Сохранённый без сети список уже показывали — баннеров по нему не делаем
        if (loaded.staleSince != null) return emptyList()
        val lastShown = store.getString(KEY)?.toIntOrNull()
        loaded.value.items.maxOfOrNull { it.id }?.let { newest -> store.putString(KEY, maxOf(newest, lastShown ?: 0).toString()) }
        // При первом запуске не засыпаем баннерами за всю историю — только отмечаем, с чего начинать
        if (lastShown == null) return emptyList()
        return loaded.value.items.filter { !it.read && it.id > lastShown }.sortedBy { it.id }
    }

    private companion object {
        const val KEY = "last_notified_id"
    }
}
