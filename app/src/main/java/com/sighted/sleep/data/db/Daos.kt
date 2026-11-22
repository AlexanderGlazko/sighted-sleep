package com.sighted.sleep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY sortOrder")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert
    suspend fun insertAll(tags: List<TagEntity>)
}
