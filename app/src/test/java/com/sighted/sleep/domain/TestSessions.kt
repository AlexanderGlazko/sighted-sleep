package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val KYIV: ZoneId = ZoneId.of("Europe/Kyiv")

/** Local wall-clock time in Kyiv, e.g. at("2026-11-20T23:00"). */
fun at(local: String, zone: ZoneId = KYIV): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

fun session(
    id: Long,
    start: String,
    end: String?,
    override: SessionType? = null,
    zone: ZoneId = KYIV,
) = SleepSession(
    id = id,
    start = at(start, zone),
    end = end?.let { at(it, zone) },
    zone = zone,
    typeOverride = override,
)
