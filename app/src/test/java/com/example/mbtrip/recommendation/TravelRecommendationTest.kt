package com.example.mbtrip.recommendation

import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime

class TravelRecommendationTest {
    private val date = LocalDate.of(2026, 11, 18)
    private val museum = SiheungPlaces.all.single { it.id == "sight02" }
    private val surf = SiheungPlaces.all.single { it.id == "activity01" }
    private fun preview(places: List<SiheungPlace>, before: List<Block> = emptyList(), starts: Map<String, LocalTime> = emptyMap()) =
        PlaceRecommender.previewDay(before, "trip", date, LocalTime.of(9,0), LocalTime.of(21,0), listOf(50), places, starts)

    @Test fun islandHasDiningAndTourismCandidatesWithSources() {
        val all = SiheungPlaces.all
        assertEquals(all.size, all.distinctBy { it.id }.size)
        val island = all.filter { it.area == SiheungArea.GEOBUKSEOM }
        assertTrue(island.count { it.category == PlaceCategory.RESTAURANT } >= 3)
        assertTrue(island.count { it.category == PlaceCategory.CAFE } >= 4)
        assertTrue(island.any { it.category == PlaceCategory.ATTRACTION })
        assertTrue(island.any { it.category == PlaceCategory.EXPERIENCE })
        assertTrue(all.all { it.sourceUrl.startsWith("https://") })
    }
    @Test fun regularMondayAndNewYearClosuresAreFiltered() {
        val monday = PlaceRecommender.recommend(PlaceCategory.ATTRACTION, SiheungArea.GEOBUKSEOM, 240, date = LocalDate.of(2026,11,23))
        assertFalse(monday.any { it.id == museum.id })
        assertTrue(monday.any { it.id == "sight01" })
        val newYear = PlaceRecommender.recommend(PlaceCategory.ATTRACTION, null, 240, date = LocalDate.of(2027,1,1))
        assertFalse(newYear.any { it.id == museum.id })
    }
    @Test fun museumIsPlacedWithinPublishedHours() {
        val visit = preview(listOf(museum)).single { it.place == museum.name }
        assertEquals(LocalTime.of(10,0), visit.start)
        assertTrue(visit.end <= LocalTime.of(18,0))
        assertEquals(museum.address, visit.address)
    }
    @Test fun cannotFitAfterClosingEvenWhenLaterFreeTimeExists() {
        val protected = Block("note", "trip", date, LocalTime.of(9,0), LocalTime.of(17,30),
            BlockKind.FREE, BlockStatus.CONFIRMED, BlockSource.USER, memo = "기존 자유시간 메모")
        val result = preview(listOf(museum), listOf(protected))
        assertTrue(result.contains(protected))
        assertFalse(result.any { it.place == museum.name })
    }
    @Test fun closedDayCannotBypassTheFilterUsingRawCandidates() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaceRecommender.previewDay(emptyList(), "trip", LocalDate.of(2026,11,23), LocalTime.of(9,0), LocalTime.of(21,0), listOf(50), listOf(museum))
        }
    }
    @Test fun bookedExperienceRequiresItsTimeAndNeverMovesToAnotherSlot() {
        assertThrows(IllegalArgumentException::class.java) { preview(listOf(surf)) }
        val visit = preview(listOf(surf), starts = mapOf(surf.id to LocalTime.of(14,0))).single { it.place == surf.name }
        assertEquals(LocalTime.of(14,0), visit.start)
        assertEquals(LocalTime.of(16,0), visit.end)
        val protected = Block("booking", "trip", date, LocalTime.of(13,0), LocalTime.of(15,0),
            BlockKind.FIXED, BlockStatus.CONFIRMED, BlockSource.USER, "예약", locked = true)
        val result = preview(listOf(surf), listOf(protected), mapOf(surf.id to LocalTime.of(14,0)))
        assertTrue(result.contains(protected)); assertFalse(result.any { it.place == surf.name })
    }
    @Test fun experienceCannotWrapPastMidnight() {
        assertThrows(IllegalArgumentException::class.java) { preview(listOf(surf), starts = mapOf(surf.id to LocalTime.of(23,30))) }
    }
    @Test fun externalLinksUseCorrectHostsAndEncodedQueries() {
        val place = SiheungPlaces.all.first().copy(name = "카페 A&B #1")
        val urls = listOf(PlaceLinks.googleMaps(place), PlaceLinks.naverMap(place), PlaceLinks.naverBlog(place))
        assertEquals(listOf("www.google.com", "map.naver.com", "search.naver.com"), urls.map { URI(it).host })
        assertTrue(urls.all { !it.contains(" ") && !it.contains("#1") && it.contains("%26") })
    }
}
