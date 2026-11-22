package com.sighted.sleep.data

import android.database.sqlite.SQLiteConstraintException
import com.sighted.sleep.data.db.TagDao
import com.sighted.sleep.data.db.TagEntity
import com.sighted.sleep.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(private val dao: TagDao) {

    /** All tags, archived included, in display order. */
    val tags: Flow<List<Tag>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Returns an error message, or null on success. */
    suspend fun add(name: String, colorHex: String): String? {
        val clean = name.trim()
        checkName(clean, ownId = null)?.let { return it }
        return try {
            dao.insert(
                TagEntity(name = clean, colorHex = colorHex, isPreset = false, sortOrder = dao.maxSortOrder() + 1),
            )
            null
        } catch (e: SQLiteConstraintException) {
            duplicate(clean)
        }
    }

    /** Rename, recolour or (un)archive. Returns an error message, or null on success. */
    suspend fun update(tag: Tag): String? {
        val clean = tag.name.trim()
        checkName(clean, ownId = tag.id)?.let { return it }
        return try {
            dao.update(
                TagEntity(
                    id = tag.id,
                    name = clean,
                    colorHex = tag.colorHex,
                    isPreset = tag.isPreset,
                    isArchived = tag.isArchived,
                    sortOrder = tag.sortOrder,
                ),
            )
            null
        } catch (e: SQLiteConstraintException) {
            duplicate(clean)
        }
    }

    /**
     * Deletes a custom tag that no sleep uses. Preset or used tags can only be archived,
     * so past check-ins never lose information. Returns an error message, or null on success.
     */
    suspend fun delete(tag: Tag): String? {
        if (tag.isPreset) return "Preset tags can't be deleted. Archive it instead."
        val uses = dao.usageCount(tag.id)
        if (uses > 0) return "Used by $uses sleep${if (uses == 1) "" else "s"}. Archive it instead."
        return if (dao.deleteIfUnused(tag.id) == 1) null else "Couldn't delete this tag."
    }

    /** Names must be non-empty and unique ignoring case ("Stress" == "stress"). */
    private suspend fun checkName(name: String, ownId: Long?): String? = when {
        name.isEmpty() -> "Tag name can't be empty."
        dao.getAll().any { it.id != ownId && it.name.equals(name, ignoreCase = true) } -> duplicate(name)
        else -> null
    }

    private fun duplicate(name: String) = "A tag named \"$name\" already exists."
}

private fun TagEntity.toDomain() = Tag(id, name, colorHex, isPreset, isArchived, sortOrder)
