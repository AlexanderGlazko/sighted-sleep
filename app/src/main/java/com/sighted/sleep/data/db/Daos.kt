package com.sighted.sleep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_session ORDER BY startUtc DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_session ORDER BY startUtc DESC")
    suspend fun getAll(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_session WHERE endUtc IS NULL LIMIT 1")
    fun observeActive(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_session WHERE endUtc IS NULL LIMIT 1")
    suspend fun getActive(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session WHERE id = :id")
    suspend fun getById(id: Long): SleepSessionEntity?

    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Query("DELETE FROM sleep_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sleep_session")
    suspend fun count(): Int

    @Query("DELETE FROM sleep_session WHERE comment = :comment")
    suspend fun deleteByComment(comment: String): Int
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY sortOrder")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT COUNT(*) FROM session_tag WHERE tagId = :tagId")
    suspend fun usageCount(tagId: Long): Int

    /** Only custom tags that no session uses can be deleted; returns rows deleted (0 or 1). */
    @Query(
        "DELETE FROM tag WHERE id = :tagId AND isPreset = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM session_tag WHERE tagId = :tagId)",
    )
    suspend fun deleteIfUnused(tagId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tag")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Insert
    suspend fun insertAll(tags: List<TagEntity>)
}

/** Morning check-in data: quality, comment and tags of a session (spec F2, F3.3). */
@Dao
abstract class CheckInDao {
    @Query("SELECT * FROM session_tag")
    abstract fun observeAllSessionTags(): Flow<List<SessionTagEntity>>

    @Query("SELECT tagId FROM session_tag WHERE sessionId = :sessionId")
    abstract suspend fun getTagIds(sessionId: Long): List<Long>

    @Query("UPDATE sleep_session SET quality = :quality, comment = :comment, updatedAt = :updatedAt WHERE id = :sessionId")
    protected abstract suspend fun updateCheckIn(sessionId: Long, quality: Int?, comment: String?, updatedAt: Long)

    @Query("DELETE FROM session_tag WHERE sessionId = :sessionId")
    protected abstract suspend fun clearTags(sessionId: Long)

    @Insert
    protected abstract suspend fun insertTags(tags: List<SessionTagEntity>)

    /** Replaces quality, comment and the full tag set of a session atomically. */
    @Transaction
    open suspend fun saveCheckIn(sessionId: Long, quality: Int?, comment: String?, tagIds: Set<Long>, updatedAt: Long) {
        updateCheckIn(sessionId, quality, comment, updatedAt)
        clearTags(sessionId)
        if (tagIds.isNotEmpty()) insertTags(tagIds.map { SessionTagEntity(sessionId, it) })
    }
}
