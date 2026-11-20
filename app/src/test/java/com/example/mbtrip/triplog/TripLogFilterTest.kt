package com.example.mbtrip.triplog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripLogFilterTest {
    private fun log(title: String, start: String, end: String, place: String = "시흥") = TripLog(
        tripId = title, authorId = "u", title = title, content = "본문 $title", satisfaction = 4,
        destination = place, startDate = LocalDate.parse(start), endDate = LocalDate.parse(end), photos = listOf("res:photo1"),
    )

    private val april = log("거북섬", "2026-04-11", "2026-04-13", "시흥 거북섬")
    private val june = log("오이도", "2026-06-06", "2026-06-07", "시흥 오이도")
    private val all = listOf(april, june)

    @Test fun 검색어_없고_기간_없으면_전부() = assertEquals(all, all.filterLogs("", null))

    @Test fun 제목_장소로_검색() {
        assertEquals(listOf(june), all.filterLogs("오이", null))
        assertEquals(listOf(april), all.filterLogs("  거북섬 ", null))
    }

    @Test fun 기간이_하루라도_겹치면_보인다() {
        val range = LogDateRange(LocalDate.parse("2026-04-13"), LocalDate.parse("2026-04-30"))
        assertEquals(listOf(april), all.filterLogs("", range))
    }

    @Test fun 기간_밖이면_안_보인다() {
        val range = LogDateRange(LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31"))
        assertTrue(all.filterLogs("", range).isEmpty())
    }

    @Test fun 날짜_글자() {
        assertEquals("2026.04.11 – 04.13", formatDateRange(april.startDate, april.endDate))
        assertEquals("2박 3일", nightsAndDays(april.startDate, april.endDate))
        assertEquals("당일치기", nightsAndDays(april.startDate, april.startDate))
    }

    @Test fun 달마다_제목과_사진칸() {
        val items = buildGalleryItems(all.sortedForGallery())
        assertEquals(4, items.size) // 6월 제목, 오이도 사진, 4월 제목, 거북섬 사진
        assertTrue(items[0] is GalleryItem.Header)
        assertEquals("2026년 6월", (items[0] as GalleryItem.Header).label)
    }
}
