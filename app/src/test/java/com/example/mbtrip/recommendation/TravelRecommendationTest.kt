package com.example.mbtrip.recommendation

import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class TravelRecommendationTest {
    @Test fun islandHasOnlyDiningCandidatesWithSources() {
        val island = SiheungPlaces.all.filter { it.area == SiheungArea.GEOBUKSEOM }
        assertEquals(7, island.size)
        assertEquals(3, island.count { it.category == PlaceCategory.RESTAURANT })
        assertEquals(4, island.count { it.category == PlaceCategory.CAFE })
        assertTrue(island.all { it.sourceUrl.startsWith("https://") })
    }
    @Test fun verifiedAddressSurvivesRecommendationAndMapLink() {
        val place = SiheungPlaces.all.single { it.id == "food22" }
        val blocks = PlaceRecommender.previewDay(emptyList(), "trip", java.time.LocalDate.of(2026,11,18),
            java.time.LocalTime.of(9,0), java.time.LocalTime.of(21,0), listOf(50), listOf(place))
        assertEquals(place.address, blocks.single { it.place == place.name }.address)
        val query = java.net.URLDecoder.decode(URI(PlaceLinks.googleMaps(place)).rawQuery, "UTF-8")
        assertTrue(query.contains("거북섬로 110"))
        assertFalse(query.contains("거북섬1로 4"))
        assertEquals("m.place.naver.com", URI(place.sourceUrl).host)
    }
    @Test fun externalLinksUseCorrectHostsAndEncodedQueries() {
        val place = SiheungPlaces.all.first().copy(name = "카페 A&B #1")
        val urls = listOf(PlaceLinks.googleMaps(place), PlaceLinks.naverMap(place), PlaceLinks.naverBlog(place))
        assertEquals(listOf("www.google.com", "map.naver.com", "m.search.naver.com"), urls.map { URI(it).host })
        assertTrue(URI(PlaceLinks.naverBlog(place)).rawQuery.startsWith("ssc=tab.m_blog.all&query="))
        assertTrue(urls.all { !it.contains(" ") && !it.contains("#1") && it.contains("%26") })
    }
}
