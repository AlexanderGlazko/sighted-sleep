package com.sighted.sleep.domain.backup

import com.sighted.sleep.domain.at
import com.sighted.sleep.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private fun s(id: Long, start: String, end: String?, quality: Int? = null, comment: String? = null) = BackupSession(
        id = id,
        startUtc = at(start).toEpochMilli(),
        endUtc = end?.let { at(it).toEpochMilli() },
        zoneId = "Europe/Kyiv",
        quality = quality,
        comment = comment,
        createdAt = 1, updatedAt = 2,
    )

    private val sample = BackupFile(
        exportedAt = "2026-11-23T08:00:00Z",
        settings = BackupSettings(22 * 60, 6 * 60, listOf("MONDAY"), BackupReminders(bedtimeEnabled = true, bedtimeOffsetMin = 15)),
        tags = listOf(BackupTag(1, "Stress", "#E57373", true, false, 0), BackupTag(12, "Blackout", "#90A4AE", false, false, 11)),
        sessions = listOf(
            s(10, "2026-11-21T23:00", "2026-11-22T07:00", 8, "slept ok"),
            s(11, "2026-11-22T14:00", "2026-11-22T14:30"),
            s(12, "2026-11-22T23:10", null),
        ),
        sessionTags = listOf(BackupSessionTag(10, 1), BackupSessionTag(10, 12)),
    )

    private fun invalid(f: BackupFile) = (BackupCodec.decode(BackupCodec.encode(f)) as DecodeResult.Invalid).reason

    @Test
    fun `round trip keeps everything`() {
        val result = BackupCodec.decode(BackupCodec.encode(sample))
        assertEquals(DecodeResult.Ok(sample), result)
    }

    @Test
    fun `unknown fields are ignored and missing settings use defaults`() {
        val text = """{"app":"Sighted Sleep","schemaVersion":1,"exportedAt":"x","futureField":42,"sessions":[]}"""
        val ok = BackupCodec.decode(text) as DecodeResult.Ok
        assertEquals(BackupSettings(), ok.file.settings)
    }

    @Test
    fun `garbage and other apps are rejected`() {
        assertTrue(BackupCodec.decode("not json") is DecodeResult.Invalid)
        assertTrue(BackupCodec.decode("""{"app":"Other","exportedAt":"x"}""") is DecodeResult.Invalid)
    }

    @Test
    fun `newer format is rejected`() {
        assertTrue(invalid(sample.copy(schemaVersion = 2)).contains("newer version"))
    }

    @Test
    fun `invalid sessions are rejected`() {
        assertTrue(invalid(sample.copy(sessions = listOf(s(1, "2026-11-22T07:00", "2026-11-22T06:00")))).contains("ends before"))
        assertTrue(invalid(sample.copy(sessions = listOf(s(1, "2026-11-22T01:00", "2026-11-22T06:00", quality = 11)), sessionTags = emptyList())).contains("quality"))
        val overlap = listOf(s(1, "2026-11-22T01:00", "2026-11-22T06:00"), s(2, "2026-11-22T05:00", "2026-11-22T08:00"))
        assertTrue(invalid(sample.copy(sessions = overlap, sessionTags = emptyList())).contains("overlapping"))
        val twoActive = listOf(s(1, "2026-11-22T01:00", null), s(2, "2026-11-23T01:00", null))
        assertTrue(invalid(sample.copy(sessions = twoActive, sessionTags = emptyList())).isNotEmpty())
    }

    @Test
    fun `references and tag names are checked`() {
        assertTrue(invalid(sample.copy(sessionTags = listOf(BackupSessionTag(10, 99)))).contains("aren't in it"))
        val dupNames = sample.tags + BackupTag(13, "stress", "#000000", false, false, 12)
        assertTrue(invalid(sample.copy(tags = dupNames)).contains("duplicate tag names"))
    }

    @Test
    fun `csv has local times, tags and escaped comments`() {
        val f = sample.copy(sessions = sample.sessions.take(2).map { if (it.id == 10L) it.copy(comment = "late, \"noisy\"") else it })
        val csv = BackupCodec.sessionsCsv(f, mapOf(10L to SessionType.MAIN, 11L to SessionType.NAP)).lines()
        assertEquals("start,end,timezone,duration_min,type,quality,tags,comment", csv[0])
        assertEquals("2026-11-21 23:00,2026-11-22 07:00,Europe/Kyiv,480,main,8,Blackout; Stress,\"late, \"\"noisy\"\"\"", csv[1])
        assertEquals("2026-11-22 14:00,2026-11-22 14:30,Europe/Kyiv,30,nap,,,", csv[2])
    }
}
