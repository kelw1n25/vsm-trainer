package ru.vsm.trainer

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.vsm.trainer.core.Loaded
import ru.vsm.trainer.data.local.InMemoryKeyValueStore
import ru.vsm.trainer.data.remote.dto.AppNotification
import ru.vsm.trainer.data.remote.dto.NotificationList
import ru.vsm.trainer.domain.NotificationSync

class NotificationSyncTest {
    private fun item(id: Int, read: Boolean = false) = AppNotification(id, "new_scenario", "Новый сценарий", "…", Instant.now(), read)
    private val repository = FakeTrainerRepository()
    private val sync = NotificationSync(repository, InMemoryKeyValueStore())

    @Test
    fun firstSyncOnlyRemembersPosition() = runTest {
        repository.notificationsResult = Loaded(NotificationList(2, listOf(item(1), item(2))))
        assertEquals(emptyList<AppNotification>(), sync.fresh())
    }

    @Test
    fun onlyNewUnreadAreShownOnce() = runTest {
        repository.notificationsResult = Loaded(NotificationList(1, listOf(item(1))))
        sync.fresh()
        repository.notificationsResult = Loaded(NotificationList(3, listOf(item(4), item(3, read = true), item(2), item(1))))
        assertEquals(listOf(2, 4), sync.fresh().map { it.id })
        assertEquals(emptyList<Int>(), sync.fresh().map { it.id })
    }

    @Test
    fun cachedListProducesNoBanners() = runTest {
        repository.notificationsResult = Loaded(NotificationList(0, listOf(item(1))))
        sync.fresh()
        repository.notificationsResult = Loaded(NotificationList(1, listOf(item(5))), staleSince = Instant.now())
        assertEquals(emptyList<AppNotification>(), sync.fresh())
    }
}
