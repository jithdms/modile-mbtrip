package com.example.mbtrip.diagnosis

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockStatus
import java.time.LocalDate
import java.time.LocalTime

/** J-P 점수 차이를 확정/자유 시간 비율로 바꾸는 규칙. jpScore는 0=J, 100=P. */
data class AllocationRatio(val fixedPercent: Int, val freePercent: Int) {
    init { require(fixedPercent + freePercent == 100) }
}

object ItineraryAllocator {
    fun ratioFor(jpScores: List<Int>): AllocationRatio {
        require(jpScores.isNotEmpty() && jpScores.all { it in 0..100 })
        val gap = (jpScores.maxOrNull() ?: 0) - (jpScores.minOrNull() ?: 0)
        return when {
            gap <= 15 -> AllocationRatio(60, 40)
            gap <= 35 -> AllocationRatio(40, 60)
            else -> AllocationRatio(25, 75)
        }
    }

    /**
     * 필수 블록을 먼저 시간표에 고정한 뒤 남은 구간에 선택 확정 블록을 배치하고,
     * 남은 시간은 자유 블록으로 채운다. 기존 필수 블록은 변경하지 않는다.
     * candidates는 우선순위 순서이며 duration은 분 단위다.
     */
    fun allocateDay(
        tripId: String,
        date: LocalDate,
        dayStart: LocalTime,
        dayEnd: LocalTime,
        jpScores: List<Int>,
        requiredBlocks: List<Block>,
        candidates: List<PlannedActivity>,
    ): List<Block> {
        require(dayStart < dayEnd)
        require(requiredBlocks.all { it.date == date && it.tripId == tripId })
        require(requiredBlocks.all { it.start >= dayStart && it.end <= dayEnd })
        val ratio = ratioFor(jpScores)
        val dayMinutes = java.time.Duration.between(dayStart, dayEnd).toMinutes().toInt()
        val targetFixed = dayMinutes * ratio.fixedPercent / 100
        val result = requiredBlocks.sortedBy { it.start }.toMutableList()
        require(noOverlap(result)) { "필수 일정끼리 시간이 겹쳐요." }
        var fixedMinutes = result.filter { it.kind == BlockKind.FIXED }.sumOf { minutes(it.start, it.end) }
        var serial = 0

        for (activity in candidates) {
            if (fixedMinutes >= targetFixed) break
            val duration = activity.durationMinutes
            if (targetFixed - fixedMinutes < duration) continue
            val slot = firstGap(result, dayStart, dayEnd, duration) ?: continue
            result += Block(
                id = "auto_${date}_${serial++}", tripId = tripId, date = date,
                start = slot, end = slot.plusMinutes(duration.toLong()), kind = BlockKind.FIXED,
                status = BlockStatus.PROPOSED, source = BlockSource.AUTO, title = activity.title,
                place = activity.place,
            )
            fixedMinutes += duration
        }

        val occupied = result.sortedBy { it.start }
        var cursor = dayStart
        var freeIndex = 0
        for (block in occupied) {
            if (cursor < block.start) result += freeBlock(tripId, date, cursor, block.start, freeIndex++)
            if (cursor < block.end) cursor = block.end
        }
        if (cursor < dayEnd) result += freeBlock(tripId, date, cursor, dayEnd, freeIndex)
        return result.sortedBy { it.start }
    }

    private fun firstGap(blocks: List<Block>, start: LocalTime, end: LocalTime, duration: Int): LocalTime? {
        var cursor = start
        for (block in blocks.sortedBy { it.start }) {
            if (java.time.Duration.between(cursor, block.start).toMinutes() >= duration) return cursor
            if (cursor < block.end) cursor = block.end
        }
        return cursor.takeIf { java.time.Duration.between(it, end).toMinutes() >= duration }
    }

    private fun freeBlock(tripId: String, date: LocalDate, start: LocalTime, end: LocalTime, index: Int) = Block(
        id = "free_${date}_$index", tripId = tripId, date = date, start = start, end = end,
        kind = BlockKind.FREE, status = BlockStatus.CONFIRMED, source = BlockSource.AUTO,
    )

    private fun minutes(start: LocalTime, end: LocalTime) = java.time.Duration.between(start, end).toMinutes().toInt()
    private fun noOverlap(blocks: List<Block>): Boolean = blocks.sortedBy { it.start }.zipWithNext().all { (a, b) -> a.end <= b.start }
}

data class PlannedActivity(val title: String, val durationMinutes: Int, val place: String? = null) {
    init { require(title.isNotBlank() && durationMinutes > 0) }
}
