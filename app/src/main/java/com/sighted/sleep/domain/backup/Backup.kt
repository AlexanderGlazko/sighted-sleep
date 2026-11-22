package com.sighted.sleep.domain.backup

import com.sighted.sleep.domain.model.SessionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Full-data backup file (spec §7 export format, F7.2/F7.3). Mirrors the database 1:1. */
@Serializable
data class BackupFile(
    val app: String = APP_NAME,
    val schemaVersion: Int = CURRENT_SCHEMA,
    /** ISO-8601 instant. */
    val exportedAt: String,
    val settings: BackupSettings = BackupSettings(),
    val tags: List<BackupTag> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    val sessionTags: List<BackupSessionTag> = emptyList(),
) {
    companion object {
        const val APP_NAME = "Sighted Sleep"
        const val CURRENT_SCHEMA = 1
    }
}

@Serializable
data class BackupSettings(
    val targetBedtimeMin: Int = 23 * 60,
    val targetWakeMin: Int = 7 * 60,
    val workDays: List<String> = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
    val reminders: BackupReminders = BackupReminders(),
)

@Serializable
data class BackupReminders(
    val bedtimeEnabled: Boolean = false,
    val bedtimeOffsetMin: Int = 30,
    val morningEnabled: Boolean = false,
    val morningDelayMin: Int = 60,
    val stillSleepingEnabled: Boolean = false,
)

@Serializable
data class BackupTag(
    val id: Long,
    val name: String,
    val colorHex: String,
    val isPreset: Boolean,
    val isArchived: Boolean = false,
    val sortOrder: Int,
)

@Serializable
data class BackupSession(
    val id: Long,
    val startUtc: Long,
    val endUtc: Long? = null,
    val zoneId: String,
    val typeOverride: String? = null,
    val quality: Int? = null,
    val comment: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSessionTag(val sessionId: Long, val tagId: Long)

sealed interface DecodeResult {
    data class Ok(val file: BackupFile) : DecodeResult
    data class Invalid(val reason: String) : DecodeResult
}

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true // newer files with extra fields still import
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** Parses and validates; never throws. Nothing is imported unless this returns [DecodeResult.Ok]. */
    fun decode(text: String): DecodeResult {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            return DecodeResult.Invalid("This isn't a Sighted Sleep backup file.")
        }
        return validate(file)?.let { DecodeResult.Invalid(it) } ?: DecodeResult.Ok(file)
    }

    /** Returns a user-facing reason the file can't be imported, or null if it's fine. */
    fun validate(f: BackupFile): String? {
        if (f.app != BackupFile.APP_NAME) return "This isn't a Sighted Sleep backup file."
        if (f.schemaVersion !in 1..BackupFile.CURRENT_SCHEMA) {
            return "This backup was made by a newer version of the app (format ${f.schemaVersion})."
        }
        if (f.sessions.map { it.id }.toSet().size != f.sessions.size) return "Backup has duplicate sleep ids."
        if (f.tags.map { it.id }.toSet().size != f.tags.size) return "Backup has duplicate tag ids."
        if (f.tags.map { it.name.trim().lowercase() }.toSet().size != f.tags.size) return "Backup has duplicate tag names."
        f.sessions.forEach { s ->
            if (s.endUtc != null && s.endUtc <= s.startUtc) return "A sleep in the backup ends before it starts."
            if (s.quality != null && s.quality !in 1..10) return "A sleep in the backup has an invalid quality score."
            if (runCatching { ZoneId.of(s.zoneId) }.isFailure) return "A sleep in the backup has an unknown time zone."
            if (s.typeOverride != null && s.typeOverride !in SessionType.entries.map { it.name }) {
                return "A sleep in the backup has an unknown type."
            }
        }
        if (f.sessions.count { it.endUtc == null } > 1) return "Backup has more than one running sleep."
        val sorted = f.sessions.sortedBy { it.startUtc }
        sorted.zipWithNext().forEach { (a, b) ->
            val aEnd = a.endUtc ?: Long.MAX_VALUE
            if (b.startUtc < aEnd) return "Backup has overlapping sleeps."
        }
        val sessionIds = f.sessions.map { it.id }.toSet()
        val tagIds = f.tags.map { it.id }.toSet()
        if (f.sessionTags.any { it.sessionId !in sessionIds || it.tagId !in tagIds }) {
            return "Backup refers to sleeps or tags that aren't in it."
        }
        return null
    }

    private val csvTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * Sessions as CSV for spreadsheets (spec F7.2). Local times in each sleep's own zone.
     * Columns: start,end,timezone,duration_min,type,quality,tags,comment
     */
    fun sessionsCsv(f: BackupFile, types: Map<Long, SessionType>): String {
        val tagNames = f.tags.associate { it.id to it.name }
        val tagsBySession = f.sessionTags.groupBy({ it.sessionId }, { tagNames[it.tagId].orEmpty() })
        val sb = StringBuilder("start,end,timezone,duration_min,type,quality,tags,comment\n")
        f.sessions.sortedBy { it.startUtc }.forEach { s ->
            val zone = ZoneId.of(s.zoneId)
            fun local(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).format(csvTime)
            val row = listOf(
                local(s.startUtc),
                s.endUtc?.let(::local).orEmpty(),
                s.zoneId,
                s.endUtc?.let { ((it - s.startUtc) / 60_000).toString() }.orEmpty(),
                types[s.id]?.name?.lowercase().orEmpty(),
                s.quality?.toString().orEmpty(),
                tagsBySession[s.id].orEmpty().sorted().joinToString("; "),
                s.comment.orEmpty(),
            )
            sb.append(row.joinToString(",") { csvField(it) }).append('\n')
        }
        return sb.toString()
    }

    /** Quotes a field when it contains a comma, quote or line break (RFC 4180). */
    fun csvField(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
