package com.sighted.sleep.data

import com.sighted.sleep.data.db.SleepSessionDao
import com.sighted.sleep.data.db.SleepSessionEntity
import com.sighted.sleep.domain.SessionValidator
import com.sighted.sleep.domain.ValidationError
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/**
 * Single entry point for reading and changing sleep sessions.
 * All writes are validated (spec §3.6) and serialised with a mutex,
 * so the app and (later) the widget can't create two active sessions.
 */
class SleepRepository(
    private val dao: SleepSessionDao,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val writeLock = Mutex()

    val sessions: Flow<List<SleepSession>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    val activeSession: Flow<SleepSession?> = dao.observeActive().map { it?.toDomain() }

    fun now(): Instant = clock()

    suspend fun getSession(id: Long): SleepSession? = dao.getById(id)?.toDomain()

    /** Starts a new active session now. Returns an error if one is already running. */
    suspend fun startSleep(): ValidationError? = writeLock.withLock {
        val now = clock()
        val existing = dao.getAll().map { it.toDomain() }
        SessionValidator.validate(null, now, null, existing, now)?.let { return it }
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
    suspend fun stopSleep(): SleepSession? = writeLock.withLock {
        val active = dao.getActive() ?: return null
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
    ): ValidationError? = writeLock.withLock {
        val now = clock()
        val existing = dao.getAll().map { it.toDomain() }
        SessionValidator.validate(id, start, end, existing, now)?.let { return it }
        val nowMs = now.toEpochMilli()
        val current = id?.let { dao.getById(it) }
        if (current == null) {
            dao.insert(
                SleepSessionEntity(
                    startUtc = start.toEpochMilli(),
                    endUtc = end?.toEpochMilli(),
                    zoneId = zone().id,
                    typeOverride = typeOverride?.name,
                    createdAt = nowMs,
                    updatedAt = nowMs,
                ),
            )
        } else {
            dao.update(
                current.copy(
                    startUtc = start.toEpochMilli(),
                    endUtc = end?.toEpochMilli(),
                    typeOverride = typeOverride?.name,
                    updatedAt = nowMs,
                ),
            )
        }
        null
    }

    suspend fun delete(id: Long) = writeLock.withLock { dao.deleteById(id) }
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
