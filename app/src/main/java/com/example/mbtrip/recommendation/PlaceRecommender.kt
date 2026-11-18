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
        date: LocalDate? = null,
    ): List<SiheungPlace> {
        require(availableMinutes >= 0)
        return places.filter {
            it.category == category && (area == null || it.area == area) &&
                it.durationMinutes <= availableMinutes && it.name !in excludedNames && (date == null || !it.regularlyClosedOn(date))
        }.sortedWith(compareBy<SiheungPlace> { it.rank }.thenBy { it.id })
    }

    /** Preserve reservations, votes and free-time notes; reuse empty automatically generated free time. */
    fun previewDay(
        existing: List<Block>, tripId: String, date: LocalDate,
        start: LocalTime, end: LocalTime, jpScores: List<Int>,
        candidates: List<SiheungPlace>,
        reservationStarts: Map<String, LocalTime> = emptyMap(),
    ): List<Block> {
        require(existing.all { it.tripId == tripId })
        val required = existing.filter { it.date == date }.filterNot {
            it.kind == BlockKind.FREE && it.source == BlockSource.AUTO && it.isEmptyFree && !it.locked
        }
        val existingNames = required.mapNotNull { it.place }.toSet()
        val unique = candidates.distinctBy { it.id }.filter { it.name !in existingNames }
        val activities = unique.map {
            require(!it.regularlyClosedOn(date)) { "${it.name}은 정기 휴관일이에요. 공식 운영 공지를 확인해주세요." }
            val reserved = reservationStarts[it.id]
            require(!it.needsReservationTime || reserved != null) { "${it.name}의 예약 시작 시각을 입력해주세요." }
            val earliest = reserved ?: it.hours?.opens ?: LocalTime.MIN
            val latest = if (reserved != null) {
                require(reserved.toSecondOfDay() + it.durationMinutes * 60 < 86400) { "체험 시간은 자정을 넘길 수 없어요." }
                reserved.plusMinutes(it.durationMinutes.toLong())
            } else it.hours?.closes ?: LocalTime.MAX
            require(reserved == null || (earliest >= start && latest <= end)) { "예약 시간을 추천 시간대 안으로 입력해주세요." }
            require(reserved == null || it.hours == null || (earliest >= it.hours.opens && latest <= it.hours.closes)) { "예약 시간이 정기 운영시간을 벗어났어요." }
            PlannedActivity(it.name, it.durationMinutes, it.name, earliest, latest)
        }
        val allocated = ItineraryAllocator.allocateDay(tripId, date, start, end, jpScores, required, activities)
        val existingIds = required.map { it.id }.toSet()
        val byName = unique.associateBy { it.name }
        return allocated.map { block ->
            val place = byName[block.place]
            if (block.id !in existingIds && block.kind == BlockKind.FIXED && place != null) {
                block.copy(source = BlockSource.RECOMMEND, tags = listOf(place.category.label),
                    address = place.address,
                    memo = "예상 체류 ${place.durationMinutes}분 · 이동시간은 별도 확인해주세요.\n${place.visitNote.orEmpty()}\n${place.sourceUrl}")
            } else block
        }
    }

    fun addedMinutes(before: List<Block>, after: List<Block>): Int {
        val ids = before.map { it.id }.toSet()
        return after.filter { it.id !in ids && it.kind == BlockKind.FIXED }
            .sumOf { Duration.between(it.start, it.end).toMinutes().toInt() }
    }
}
