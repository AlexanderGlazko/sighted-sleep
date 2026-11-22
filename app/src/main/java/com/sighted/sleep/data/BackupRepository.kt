package com.sighted.sleep.data

import androidx.room.withTransaction
import com.sighted.sleep.data.db.SessionTagEntity
import com.sighted.sleep.data.db.SightedSleepDatabase
import com.sighted.sleep.data.db.SleepSessionEntity
import com.sighted.sleep.data.db.TagEntity
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.backup.BackupCodec
import com.sighted.sleep.domain.backup.BackupFile
import com.sighted.sleep.domain.backup.BackupSession
import com.sighted.sleep.domain.backup.BackupSessionTag
import com.sighted.sleep.domain.backup.BackupTag
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import java.time.Instant
import java.time.ZoneId

/** Export / import of everything (spec F7.2, F7.3). */
class BackupRepository(
    private val db: SightedSleepDatabase,
    private val settings: SettingsRepository,
    private val sleepRepository: SleepRepository,
) {
    suspend fun export(now: Instant = Instant.now()): BackupFile {
        val sessions = db.sessionDao().getAll()
        val tags = db.tagDao().getAll()
        val links = db.checkInDao().getAllSessionTags()
        return BackupFile(
            exportedAt = now.toString(),
            settings = settings.toBackup(),
            tags = tags.sortedBy { it.sortOrder }.map { BackupTag(it.id, it.name, it.colorHex, it.isPreset, it.isArchived, it.sortOrder) },
            sessions = sessions.sortedBy { it.startUtc }.map {
                BackupSession(it.id, it.startUtc, it.endUtc, it.zoneId, it.typeOverride, it.quality, it.comment, it.createdAt, it.updatedAt)
            },
            sessionTags = links.map { BackupSessionTag(it.sessionId, it.tagId) },
        )
    }

    suspend fun exportJson(): String = BackupCodec.encode(export())

    suspend fun exportCsv(): String {
        val file = export()
        val domain = file.sessions.map {
            SleepSession(
                id = it.id,
                start = Instant.ofEpochMilli(it.startUtc),
                end = it.endUtc?.let(Instant::ofEpochMilli),
                zone = ZoneId.of(it.zoneId),
                typeOverride = it.typeOverride?.let(SessionType::valueOf),
            )
        }
        return BackupCodec.sessionsCsv(file, SessionClassifier.classify(domain))
    }

    /**
     * Replaces ALL data with the backup (spec F7.3). The file must already be validated by
     * [BackupCodec.decode]. Database changes run in one transaction: on any error nothing changes.
     */
    suspend fun replaceAll(file: BackupFile) {
        db.withTransaction {
            db.checkInDao().deleteAllSessionTags()
            db.sessionDao().deleteAll()
            db.tagDao().deleteAll()
            db.tagDao().insertAll(file.tags.map { TagEntity(it.id, it.name, it.colorHex, it.isPreset, it.isArchived, it.sortOrder) })
            db.sessionDao().insertAll(
                file.sessions.map {
                    SleepSessionEntity(it.id, it.startUtc, it.endUtc, it.zoneId, it.typeOverride, it.quality, it.comment, it.createdAt, it.updatedAt)
                },
            )
            db.checkInDao().insertAllSessionTags(file.sessionTags.map { SessionTagEntity(it.sessionId, it.tagId) })
        }
        settings.restore(file.settings)
        sleepRepository.notifyChanged()
    }
}
