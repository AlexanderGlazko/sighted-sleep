package com.sighted.sleep.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SleepSessionEntity::class, TagEntity::class, SessionTagEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SightedSleepDatabase : RoomDatabase() {
    abstract fun sessionDao(): SleepSessionDao
    abstract fun tagDao(): TagDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        fun create(context: Context): SightedSleepDatabase =
            Room.databaseBuilder(context, SightedSleepDatabase::class.java, "sighted_sleep.db")
                // No destructive fallback: schema changes must ship a Migration (CLAUDE.md).
                .addCallback(SeedPresetTags)
                .build()
    }
}

/** Seeds the preset tags (spec F3.1) once, when the database file is first created. */
private object SeedPresetTags : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        PRESET_TAGS.forEachIndexed { index, (name, color) ->
            db.execSQL(
                "INSERT INTO tag (name, colorHex, isPreset, isArchived, sortOrder) VALUES (?, ?, 1, 0, ?)",
                arrayOf<Any>(name, color, index),
            )
        }
    }
}

val PRESET_TAGS: List<Pair<String, String>> = listOf(
    "Caffeine late" to "#A1887F",
    "Alcohol" to "#BA68C8",
    "Late meal" to "#FFB74D",
    "Screens in bed" to "#4FC3F7",
    "Late work" to "#7986CB",
    "Exercise" to "#81C784",
    "Stress" to "#E57373",
    "Illness" to "#AED581",
    "Travel" to "#4DB6AC",
    "Social event" to "#F06292",
    "Day off" to "#FFD54F",
)
