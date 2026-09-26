package ru.vsm.trainer.core

import java.time.Duration
import java.time.Instant

/**
 * Часы телефона могут спешить или отставать. Дедлайн приходит во времени сервера, поэтому остаток
 * считаем с поправкой: смещение = время сервера из ответа − время телефона при получении.
 * Показ — задача клиента; принимать ли ответ, решает сервер.
 */
class ServerClock {
    private var offset: Duration = Duration.ZERO

    fun sync(serverTime: Instant, receivedAt: Instant) {
        offset = Duration.between(receivedAt, serverTime)
    }

    fun serverNow(local: Instant): Instant = local.plus(offset)

    fun remainingSeconds(deadline: Instant, local: Instant): Double =
        (Duration.between(serverNow(local), deadline).toMillis() / 1000.0).coerceAtLeast(0.0)
}
