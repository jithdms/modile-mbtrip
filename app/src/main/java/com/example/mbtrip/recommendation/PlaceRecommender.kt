package com.example.mbtrip.recommendation

import com.example.mbtrip.diagnosis.ItineraryAllocator
import com.example.mbtrip.diagnosis.PlannedActivity
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

object PlaceRecommender {
    /** Filter first, then use editorial priority. Never infer food preferences from J/P. */
    fun recommend(
        category: PlaceCategory,
        area: SiheungArea?,
        availableMinutes: Int,
        excludedNames: Set<String> = emptySet(),
        places: List<SiheungPlace> = SiheungPlaces.all,
    ): List<SiheungPlace> {
        require(availableMinutes >= 0)
        return places.filter {
            it.category == category && (area == null || it.area == area) &&
                it.durationMinutes <= availableMinutes && it.name !in excludedNames
        }.sortedWith(compareBy<SiheungPlace> { it.rank }.thenBy { it.id })
    }

    /** Preserve reservations, votes and free-time notes; reuse empty automatically generated free time. */
    fun previewDay(
        existing: List<Block>, tripId: String, date: LocalDate,
        start: LocalTime, end: LocalTime, jpScores: List<Int>,
        candidates: List<SiheungPlace>,
    ): List<Block> {
        require(existing.all { it.tripId == tripId })
        val required = existing.filter { it.date == date }.filterNot {
            it.kind == BlockKind.FREE && it.source == BlockSource.AUTO && it.isEmptyFree && !it.locked
        }
        val existingNames = required.mapNotNull { it.place }.toSet()
        val unique = candidates.distinctBy { it.id }.filter { it.name !in existingNames }
        val activities = unique.map {
            PlannedActivity(it.name, it.durationMinutes, it.name)
        }
        val allocated = ItineraryAllocator.allocateDay(tripId, date, start, end, jpScores, required, activities)
        val existingIds = required.map { it.id }.toSet()
        val byName = unique.associateBy { it.name }
        return allocated.map { block ->
            val place = byName[block.place]
            if (block.id !in existingIds && block.kind == BlockKind.FIXED && place != null) {
                block.copy(source = BlockSource.RECOMMEND, tags = listOf(place.category.label),
                    memo = "예상 체류 ${place.durationMinutes}분 · 영업시간과 이동시간은 지도에서 확인해주세요.\n${place.sourceUrl}")
            } else block
        }
    }

    fun addedMinutes(before: List<Block>, after: List<Block>): Int {
        val ids = before.map { it.id }.toSet()
        return after.filter { it.id !in ids && it.kind == BlockKind.FIXED }
            .sumOf { Duration.between(it.start, it.end).toMinutes().toInt() }
    }
}
