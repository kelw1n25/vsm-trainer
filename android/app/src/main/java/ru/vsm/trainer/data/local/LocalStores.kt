package ru.vsm.trainer.data.local

import android.content.SharedPreferences
import java.io.File
import java.time.Instant

/**
 * Последний успешный ответ каждого экрана — чтобы без сети показать сохранённые данные, а не пустоту.
 * Лежит в каталоге кэша: система может его очистить, источник правды всё равно на сервере.
 */
interface ResponseCache {
    fun read(key: String): Pair<String, Instant>?
    fun write(key: String, json: String)
    fun clear()
}

class FileResponseCache(private val directory: File) : ResponseCache {
    private fun file(key: String) = File(directory.apply { mkdirs() }, "$key.json")

    override fun read(key: String): Pair<String, Instant>? {
        val file = file(key)
        return if (file.exists()) file.readText() to Instant.ofEpochMilli(file.lastModified()) else null
    }

    override fun write(key: String, json: String) {
        // Запись через временный файл: оборванная запись не оставит битый JSON
        val temp = File(directory.apply { mkdirs() }, "$key.tmp")
        temp.writeText(json)
        temp.renameTo(file(key))
    }

    override fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }
}

class InMemoryResponseCache(private val now: () -> Instant = Instant::now) : ResponseCache {
    private val entries = mutableMapOf<String, Pair<String, Instant>>()
    @Synchronized override fun read(key: String) = entries[key]
    @Synchronized override fun write(key: String, json: String) { entries[key] = json to now() }
    @Synchronized override fun clear() = entries.clear()
}

/** Небольшие настройки устройства. Секреты здесь не хранятся — для них TokenStore. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

class PreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    @Synchronized override fun getString(key: String) = values[key]
    @Synchronized override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
