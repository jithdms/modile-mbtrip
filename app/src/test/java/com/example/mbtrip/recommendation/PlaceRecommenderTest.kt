package com.example.mbtrip.recommendation

import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PlaceRecommenderTest {
    private val date = LocalDate.of(2026, 11, 17)
    private fun preview(existing: List<Block> = emptyList(), candidates: List<SiheungPlace> = SiheungPlaces.all.filter { it.category == PlaceCategory.RESTAURANT || it.category == PlaceCategory.CAFE }) =
        PlaceRecommender.previewDay(existing, "trip", date, LocalTime.of(9, 0), LocalTime.of(17, 0), listOf(0, 100), candidates)

    @Test fun catalogHasTwentyRestaurantsAndTwentyCafesWithUniqueIdsAndSources() {
        assertEquals(SiheungPlaces.all.size, SiheungPlaces.all.map { it.id }.toSet().size)
        (1..20).forEach { assertTrue(SiheungPlaces.all.any { p -> p.id == "food%02d".format(it) }); assertTrue(SiheungPlaces.all.any { p -> p.id == "cafe%02d".format(it) }) }
        listOf(PlaceCategory.RESTAURANT, PlaceCategory.CAFE).forEach { category ->
            val places = SiheungPlaces.all.filter { it.category == category }
            assertTrue(places.size >= 20)
            assertEquals((1..places.size).toList(), places.map { it.rank })
            assertTrue(places.all { it.sourceUrl.startsWith("https://") && it.mapQuery.startsWith("경기도 시흥시 ") })
        }
    }
    @Test fun categoryAreaTimeAndExclusionsAreAllRespected() {
        val excluded = "레이크우드"
        val result = PlaceRecommender.recommend(PlaceCategory.CAFE, SiheungArea.MULWANG, 40, setOf(excluded))
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.category == PlaceCategory.CAFE && it.area == SiheungArea.MULWANG && it.durationMinutes <= 40 && it.name != excluded })
        assertEquals(result.sortedBy { it.rank }, result)
    }
    @Test fun insufficientTimeReturnsNoCandidates() {
        assertTrue(PlaceRecommender.recommend(PlaceCategory.RESTAURANT, null, 59).isEmpty())
        assertTrue(PlaceRecommender.recommend(PlaceCategory.CAFE, null, 0).isEmpty())
    }
    @Test fun invalidAvailableTimeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { PlaceRecommender.recommend(PlaceCategory.CAFE, null, -1) }
    }
    @Test fun previewPreservesReservationsVotesAndFreeTimeNotes() {
        val reservation = Block("booking", "trip", date, LocalTime.of(12,0), LocalTime.of(13,0),
            BlockKind.FIXED, BlockStatus.VOTING, BlockSource.USER, "예약", locked = true, version = 4)
        val free = Block("note", "trip", date, LocalTime.of(15,0), LocalTime.of(16,0),
            BlockKind.FREE, BlockStatus.CONFIRMED, BlockSource.USER, memo = "자유롭게 쉬기")
        val before = listOf(reservation, free)
        val after = preview(before)
        assertTrue(after.containsAll(before))
        assertEquals(60, PlaceRecommender.addedMinutes(before, after))
    }
    @Test fun recommendationBlocksHaveSourcesAndStartAsProposals() {
        val after = preview()
        val recommendations = after.filter { it.source == BlockSource.RECOMMEND }
        assertEquals(2, recommendations.size)
        assertTrue(recommendations.all { it.status == BlockStatus.PROPOSED && it.tags == listOf("맛집") && it.memo!!.contains("https://") })
    }
    @Test fun duplicateCandidatesDoNotCreateDuplicateVisits() {
        val place = SiheungPlaces.all.first()
        assertEquals(1, preview(candidates = listOf(place, place)).count { it.place == place.name })
    }
    @Test fun previouslyScheduledPlacesAreExcluded() {
        val place = SiheungPlaces.all.first()
        val reservation = Block("r", "trip", date, LocalTime.of(12,0), LocalTime.of(13,0),
            BlockKind.FIXED, BlockStatus.CONFIRMED, BlockSource.USER, place.name, place.name)
        assertEquals(1, preview(listOf(reservation), listOf(place)).count { it.place == place.name })
    }
    @Test fun previewDoesNotModifyOtherDaysOrInputLists() {
        val other = Block("other", "trip", date.plusDays(1), LocalTime.of(12,0), LocalTime.of(13,0),
            BlockKind.FIXED, BlockStatus.CONFIRMED, BlockSource.USER)
        val before = listOf(other)
        val result = preview(before)
        assertEquals(listOf(other), before)
        assertTrue(result.all { it.date == date })
    }
    @Test fun blocksFromAnotherTripAreRejected() {
        val other = Block("other", "different", date, LocalTime.of(12,0), LocalTime.of(13,0),
            BlockKind.FIXED, BlockStatus.CONFIRMED, BlockSource.USER)
        assertThrows(IllegalArgumentException::class.java) { preview(listOf(other)) }
    }

    @Test fun additionalRecommendationsCanReuseEmptyAutoFreeTimeWithoutRemovingEarlierPlans() {
        val first = preview(candidates = listOf(SiheungPlaces.all.first()))
        val cafe = SiheungPlaces.all.first { it.category == PlaceCategory.CAFE }
        val second = preview(first, listOf(cafe))
        assertTrue(second.containsAll(first.filter { it.kind == BlockKind.FIXED }))
        assertEquals(40, PlaceRecommender.addedMinutes(first, second))
        second.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }
}
