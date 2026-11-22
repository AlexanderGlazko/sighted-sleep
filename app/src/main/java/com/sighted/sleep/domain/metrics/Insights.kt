package com.sighted.sleep.domain.metrics

/** Minimum sessions per group before a pattern is shown (spec §4). */
const val MIN_SAMPLES = 3

/** Average quality for main sleeps whose bedtime falls in one 30-minute bucket (spec F5.5). */
data class BedtimeBucket(
    /** Bucket start, minutes after midnight (e.g. 1380 = 23:00–23:30). */
    val startMin: Int,
    val count: Int,
    val avgQuality: Double,
) {
    val enoughData: Boolean get() = count >= MIN_SAMPLES
}

/** With vs. without one tag (spec F5.6). Null deltas = not enough data on one side. */
data class TagImpact(
    val tagId: Long,
    val withCount: Int,
    val withoutCount: Int,
    /** Avg quality with − without (rated sleeps only). */
    val qualityDelta: Double?,
    /** Avg duration with − without, minutes. */
    val durationDeltaMin: Int?,
    /** Mean midpoint with − without, minutes (positive = later). Circular. */
    val midpointShiftMin: Int?,
)

object InsightMetrics {
    const val BUCKET = 30

    /**
     * Quality by bedtime in 30-minute buckets, ordered through the night starting at 15:00
     * (so 23:30 comes before 00:00). Only rated main sleeps count.
     */
    fun qualityByBedtime(mains: List<MainSleep>): List<BedtimeBucket> =
        mains.filter { it.quality != null }
            .groupBy { it.bedtime / BUCKET * BUCKET }
            .map { (start, list) -> BedtimeBucket(start, list.size, list.map { it.quality!! }.average()) }
            .sortedBy { nightOrder(it.startMin) }

    /** Best bucket among those with enough data, or null. Ties go to the earlier bedtime. */
    fun bestBedtime(buckets: List<BedtimeBucket>): BedtimeBucket? =
        buckets.filter { it.enoughData }.maxWithOrNull(
            compareBy<BedtimeBucket> { it.avgQuality }.thenByDescending { nightOrder(it.startMin) },
        )

    /**
     * Tag impact for every tag used at least once. A metric is only computed when both groups have
     * at least [MIN_SAMPLES] sleeps (for quality: rated sleeps). Sorted by the size of the quality effect.
     */
    fun tagImpact(mains: List<MainSleep>, tagIds: Collection<Long>): List<TagImpact> =
        tagIds.mapNotNull { tagId ->
            val (with, without) = mains.partition { tagId in it.tagIds }
            if (with.isEmpty()) return@mapNotNull null
            val enough = with.size >= MIN_SAMPLES && without.size >= MIN_SAMPLES
            val qWith = with.mapNotNull { it.quality }
            val qWithout = without.mapNotNull { it.quality }
            TagImpact(
                tagId = tagId,
                withCount = with.size,
                withoutCount = without.size,
                qualityDelta = if (qWith.size >= MIN_SAMPLES && qWithout.size >= MIN_SAMPLES) qWith.average() - qWithout.average() else null,
                durationDeltaMin = if (enough) (with.avgMinutes() - without.avgMinutes()).toInt() else null,
                midpointShiftMin = if (enough) {
                    val a = CircularTime.mean(with.map { it.midpoint })
                    val b = CircularTime.mean(without.map { it.midpoint })
                    if (a != null && b != null) CircularTime.diff(a, b) else null
                } else null,
            )
        }.sortedByDescending { kotlin.math.abs(it.qualityDelta ?: 0.0) }

    /** Position of a clock time in a night that starts at 15:00. */
    private fun nightOrder(min: Int) = (min - 15 * 60 + CircularTime.DAY) % CircularTime.DAY

    private fun List<MainSleep>.avgMinutes() = map { it.duration.toMinutes() }.average()
}
