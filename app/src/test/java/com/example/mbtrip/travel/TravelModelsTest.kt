package com.example.mbtrip.travel

import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TravelModelsTest {
    private fun input(dates: String = "2026.11.17 ~ 2026.11.18", count: String = "3 명") =
        TripInput.parse(" 학교 ", " 시흥 ", dates, count)
    @Test fun validInputIsTrimmedAndParsed() {
        val result = input()
        assertEquals("학교", result.departure)
        assertEquals("시흥", result.destination)
        assertEquals(3, result.headcount)
        assertEquals(LocalDate.of(2026,11,17), result.start)
    }
    @Test fun invalidCalendarDatesReversedDatesAndExcessiveTripsAreRejected() {
        for (dates in listOf("2026.02.30 ~ 2026.03.01", "2026.11.18 ~ 2026.11.17", "2026.11.01 ~ 2026.12.02", "11월")) {
            assertThrows(IllegalArgumentException::class.java) { input(dates) }
        }
    }
    @Test fun invalidCountsAndBlankLocationsAreRejected() {
        for (count in listOf("0", "51", "-1", "두 명")) assertThrows(IllegalArgumentException::class.java) { input(count = count) }
        assertThrows(IllegalArgumentException::class.java) { TripInput.parse(" ", "시흥", "2026.11.17 ~ 2026.11.18", "3") }
    }
    @Test fun travelingIncludesStartAndEndDatesOnly() {
        val trip = ActiveTrip("actual", "uid", input())
        assertTrue(trip.isTraveling(trip.input.start))
        assertTrue(trip.isTraveling(trip.input.end))
        assertFalse(trip.isTraveling(trip.input.start.minusDays(1)))
        assertFalse(trip.isTraveling(trip.input.end.plusDays(1)))
    }
    @Test fun missingDiagnosisIsExplicitRatherThanPretendingToBeBalanced() {
        val member = MemberFields.decode("uid", mapOf("nickname" to "이름"), false)
        assertFalse(member.hasDiagnosis)
        assertEquals("@uid", member.handle)
    }
    @Test fun invalidScoresAndPhotoUrlsAreNotTreatedAsRealData() {
        for (score in listOf(-1, 101)) assertFalse(MemberFields.decode("uid", mapOf("jpScore" to score), true).hasDiagnosis)
        assertNull(MemberFields.decode("uid", mapOf("photoUrl" to "http://unsafe"), false).photoUrl)
        val member = MemberFields.decode("uid", mapOf("jpScore" to 0L, "photoUrl" to "https://example.com/a.jpg"), true)
        assertTrue(member.hasDiagnosis)
        assertEquals(0, member.jpScore)
        assertTrue(member.isTraveling)
        assertNotNull(member.photoUrl)
    }
    @Test fun everyBlockKindAndSourceRoundTripsWithoutLosingFields() {
        for (kind in BlockKind.entries) for (source in BlockSource.entries) {
            val b = Block("block", "trip", input().start, LocalTime.of(10,0), LocalTime.of(11,0), kind,
                BlockStatus.CONFIRMED, source, "제목", "장소", true, 4, listOf("맛집"), "메모", "- [ ] 할 일", Transport.WALK)
            assertEquals(b, BlockFields.decode(b.id, b.tripId, BlockFields.encode(b)))
        }
    }
    @Test fun siheungSourceIsReadAsRecommendation() {
        val b = Block("b", "trip", input().start, LocalTime.of(10,0), LocalTime.of(11,0), BlockKind.FIXED,
            BlockStatus.PROPOSED, BlockSource.RECOMMEND)
        assertEquals(BlockSource.RECOMMEND, BlockFields.decode("b", "trip", BlockFields.encode(b) + ("source" to "siheung")).source)
    }

    @Test fun schemaUsesTeamFieldNamesAndPreservesPlaceCoordinates() {
        val b = Block("b", "trip", input().start, LocalTime.of(10,0), LocalTime.of(11,0), BlockKind.FIXED,
            BlockStatus.PROPOSED, BlockSource.RECOMMEND, place = "장소", lat = 37.3, lng = 126.7,
            address = "주소", deleteRequested = true)
        val fields = BlockFields.encode(b)
        assertEquals("10:00", fields["startTime"])
        assertEquals("fixed", fields["type"])
        assertEquals("siheung", fields["source"])
        assertFalse(fields.containsKey("start"))
        assertEquals(37.3, (fields["place"] as Map<*, *>)["lat"])
        assertEquals(b, BlockFields.decode("b", "trip", fields))
    }

    @Test fun legacyFieldsAreStillReadable() {
        val fields = mapOf("date" to "2026-11-17", "start" to "10:00", "end" to "11:00",
            "kind" to "FIXED", "status" to "PROPOSED", "source" to "RECOMMEND", "place" to "식당")
        val block = BlockFields.decode("b", "trip", fields)
        assertEquals("식당", block.place)
        assertEquals(BlockSource.RECOMMEND, block.source)
        assertEquals(LocalTime.of(10,0), block.start)
    }
}
