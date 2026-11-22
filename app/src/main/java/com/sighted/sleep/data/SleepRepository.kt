package com.sighted.sleep.data

import com.sighted.sleep.data.db.CheckInDao
import com.sighted.sleep.data.db.SleepSessionDao
import com.sighted.sleep.data.db.SleepSessionEntity
import com.sighted.sleep.domain.SessionValidator
import com.sighted.sleep.domain.ValidationError
import com.sighted.sleep.domain.model.CheckIn
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.sample.SampleData
import com.sighted.sleep.domain.sample.SampleSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

sealed interface SaveOutcome {
    data class Saved(val id: Long) : SaveOutcome
    data class Invalid(val error: ValidationError) : SaveOutcome
}

/**
 * Single entry point for reading and changing sleep sessions.
 * All writes are validated (spec §3.6) and serialised with a mutex,
 * so the app and the widget can't create two active sessions.
 */
class SleepRepository(
    private val dao: SleepSessionDao,
    private val checkInDao: CheckInDao,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    /** Called after any change to sessions (used to refresh the home-screen widget). */
    private val onSessionsChanged: suspend () -> Unit = {},
) {
    private val writeLock = Mutex()

    val sessions: Flow<List<SleepSession>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    val activeSession: Flow<SleepSession?> = dao.observeActive().map { it?.toDomain() }

    /** sessionId → tag ids. */
    val sessionTags: Flow<Map<Long, Set<Long>>> = checkInDao.observeAllSessionTags().map { rows ->
        rows.groupBy({ it.sessionId }, { it.tagId }).mapValues { it.value.toSet() }
    }

    fun now(): Instant = clock()

    suspend fun getSession(id: Long): SleepSession? = dao.getById(id)?.toDomain()
    suspend fun getActive(): SleepSession? = dao.getActive()?.toDomain()

    suspend fun getCheckIn(sessionId: Long): CheckIn {
        val session = dao.getById(sessionId) ?: return CheckIn()
        return CheckIn(
            quality = session.quality,
            tagIds = checkInDao.getTagIds(sessionId).toSet(),
            comment = session.comment.orEmpty(),
        )
    }

    suspend fun saveCheckIn(sessionId: Long, checkIn: CheckIn) {
        checkInDao.saveCheckIn(
            sessionId = sessionId,
            quality = checkIn.quality?.coerceIn(1, 10),
            comment = checkIn.comment.trim().ifEmpty { null },
            tagIds = checkIn.tagIds,
            updatedAt = clock().toEpochMilli(),
        )
    }

    /** Starts a new active session now. Returns an error if one is already running. */
    suspend fun startSleep(): ValidationError? = changing {
        val now = clock()
        val existing = dao.getAll().map { it.toDomain() }
        SessionValidator.validate(null, now, null, existing, now)?.let { return@changing it }
        dao.insert(
            SleepSessionEntity(
                startUtc = now.toEpochMilli(),
                endUtc = null,
                zoneId = zone().id,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
            ),
        )
        null
    }

    /** Ends the active session now and returns it, or null if nothing was running. */
    suspend fun stopSleep(): SleepSession? = changing {
        val active = dao.getActive() ?: return@changing null
        val now = clock().toEpochMilli().coerceAtLeast(active.startUtc + 1)
        val finished = active.copy(endUtc = now, updatedAt = now)
        dao.update(finished)
        finished.toDomain()
    }

    /** Creates (id == null) or updates a session after validating it. */
    suspend fun save(
        id: Long?,
        start: Instant,
        end: Instant?,
        typeOverride: SessionType?,
    ): SaveOutcome = changing {
        val now = clock()
        val existing = dao.getAll().map { it.toDomain() }
        SessionValidator.validate(id, start, end, existing, now)?.let { return@changing SaveOutcome.Invalid(it) }
        val nowMs = now.toEpochMilli()
        val current = id?.let { dao.getById(it) }
        if (current == null) {
            val newId = dao.insert(
                SleepSessionEntity(
                    startUtc = start.toEpochMilli(),
                    endUtc = end?.toEpochMilli(),
                    zoneId = zone().id,
                    typeOverride = typeOverride?.name,
                    createdAt = nowMs,
                    updatedAt = nowMs,
                ),
            )
            SaveOutcome.Saved(newId)
        } else {
            dao.update(
                current.copy(
                    startUtc = start.toEpochMilli(),
                    endUtc = end?.toEpochMilli(),
                    typeOverride = typeOverride?.name,
                    updatedAt = nowMs,
                ),
            )
            SaveOutcome.Saved(current.id)
        }
    }

    suspend fun delete(id: Long) = changing { dao.deleteById(id) }

    /**
     * Debug only (spec F7.5): inserts generated history. Refuses if any sleep exists,
     * so sample data never mixes with real data. Returns an error message, or null on success.
     */
    suspend fun insertSampleData(sessions: List<SampleSession>, tagIdsByName: Map<String, Long>): String? = changing {
        if (dao.count() > 0) return@changing "Sample data can only be added when the log is empty."
        val nowMs = clock().toEpochMilli()
        sessions.forEach { s ->
            val id = dao.insert(
                SleepSessionEntity(
                    startUtc = s.start.toEpochMilli(),
                    endUtc = s.end.toEpochMilli(),
                    zoneId = zone().id,
                    createdAt = nowMs,
                    updatedAt = nowMs,
                ),
            )
            checkInDao.saveCheckIn(
                sessionId = id,
                quality = s.quality,
                comment = SampleData.MARKER,
                tagIds = s.tagNames.mapNotNull { tagIdsByName[it] }.toSet(),
                updatedAt = nowMs,
            )
        }
        null
    }

    /** Removes only sessions created by [insertSampleData]. Returns how many were deleted. */
    suspend fun deleteSampleData(): Int = changing { dao.deleteByComment(SampleData.MARKER) }

    /** Tells observers (widget, reminders) that data changed outside this class, e.g. after an import. */
    suspend fun notifyChanged() = onSessionsChanged()

    private suspend inline fun <T> changing(block: () -> T): T {
        val result = writeLock.withLock { block() }
        onSessionsChanged()
        return result
    }
}

private fun SleepSessionEntity.toDomain() = SleepSession(
    id = id,
    start = Instant.ofEpochMilli(startUtc),
    end = endUtc?.let(Instant::ofEpochMilli),
    zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()),
    typeOverride = typeOverride?.let { runCatching { SessionType.valueOf(it) }.getOrNull() },
    quality = quality,
    comment = comment,
)
