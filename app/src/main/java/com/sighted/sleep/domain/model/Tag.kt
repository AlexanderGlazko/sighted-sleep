package com.sighted.sleep.domain.model

data class Tag(
    val id: Long,
    val name: String,
    /** "#RRGGBB" */
    val colorHex: String,
    val isPreset: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
)

/** What the user records about a finished session in the morning (spec F2.1). */
data class CheckIn(
    /** 1..10, null = not rated. */
    val quality: Int? = null,
    val tagIds: Set<Long> = emptySet(),
    val comment: String = "",
)
