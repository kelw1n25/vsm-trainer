package ru.vsm.trainer

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.data.remote.dto.Debrief
import ru.vsm.trainer.data.remote.dto.Leaderboard
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.remote.dto.Role
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.data.remote.dto.SessionTokens

/** DTO клиента совпадают с ответами сервера: фикстуры сняты с backend, а не написаны руками. */
class ContractTest {
    @Test
    fun runStatesDecode() {
        val reading = Fixture.decode<RunState>("run_reading")
        assertEquals(RunStatus.IN_PROGRESS, reading.status)
        assertFalse(reading.node!!.choicesShown)
        assertTrue("до показа вариантов сервер их не отдаёт", reading.node!!.choices.isEmpty())

        val timed = Fixture.decode<RunState>("run_timed")
        assertEquals(20, timed.node!!.timerSeconds)
        assertEquals(Duration.ofSeconds(1), Duration.between(timed.node!!.deadlineAt, timed.node!!.timeoutAt))

        val finished = Fixture.decode<RunState>("run_finished")
        assertNull(finished.node)
        assertTrue(finished.final!!.xpEarned > 0)
        assertFalse(finished.lastSteps.first().reaction.isEmpty())
    }

    @Test
    fun microsecondsSurviveCacheRoundTrip() {
        val timed = Fixture.decode<RunState>("run_timed")
        val again = testJson.decodeFromString<RunState>(testJson.encodeToString(RunState.serializer(), timed))
        assertEquals(timed, again)
        assertEquals(Instant.parse("2026-09-26T08:39:33.731861Z"), timed.serverTime)
    }

    @Test
    fun screensDecode() {
        val profile = Fixture.decode<Profile>("profile")
        assertTrue("коды компетенций не переименовываются", "first_aid" in profile.competencePoints)
        assertEquals(14, Fixture.decode<List<ScenarioSummary>>("scenarios").size)
        assertNotNull(Fixture.decode<Leaderboard>("leaderboard").me)
        assertEquals(5, Fixture.decode<Analytics>("analytics").competences.size)
        assertEquals(Role.CONDUCTOR, Fixture.decode<SessionTokens>("login").role)
    }

    @Test
    fun debriefExplainsConsequences() {
        val debrief = Fixture.decode<Debrief>("debrief")
        assertTrue(debrief.steps.all { it.explanation != null })
        assertFalse("разбор опирается на стандарт ситуации", debrief.situations.isEmpty())
    }
}
