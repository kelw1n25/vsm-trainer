package ru.vsm.trainer.core

import ru.vsm.trainer.data.remote.dto.Role
import ru.vsm.trainer.data.remote.dto.RunStatus

/** Подписи кодов сервера. Названия компетенций приходят с сервера (`/api/meta`). */
object Labels {
    fun outcome(status: RunStatus) = when (status) {
        RunStatus.SUCCESS -> "Успех"
        RunStatus.PARTIAL -> "Частичный успех"
        RunStatus.FAILURE -> "Провал"
        RunStatus.IN_PROGRESS -> "В процессе"
    }

    fun category(code: String) = mapOf(
        "conflict" to "Конфликт", "medical" to "Медицина", "service" to "Сервис", "safety" to "Безопасность",
    )[code] ?: code

    fun role(role: Role) = when (role) {
        Role.CONDUCTOR -> "Проводник ВСМ"
        Role.INSTRUCTOR -> "Инструктор"
    }

    fun stage(code: String) = mapOf("boarding" to "На посадке", "onboard" to "В пути")[code] ?: code

    fun signed(value: Int) = if (value > 0) "+$value" else "$value"
}
