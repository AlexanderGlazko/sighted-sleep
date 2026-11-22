package com.sighted.sleep.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A sleep session (spec §7). Timestamps are UTC epoch millis; [zoneId] is captured at start. */
@Entity(
    tableName = "sleep_session",
    indices = [Index("startUtc"), Index("endUtc")],
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startUtc: Long,
    /** null = session is still active. */
    val endUtc: Long?,
    val zoneId: String,
    /** null = automatic classification; otherwise "MAIN" or "NAP". */
    val typeOverride: String? = null,
    /** 1..10, null = not rated yet. */
    val quality: Int? = null,
    val comment: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tag",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isPreset: Boolean,
    val isArchived: Boolean = false,
    val sortOrder: Int,
)

@Entity(
    tableName = "session_tag",
    primaryKeys = ["sessionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class SessionTagEntity(
    val sessionId: Long,
    val tagId: Long,
)
