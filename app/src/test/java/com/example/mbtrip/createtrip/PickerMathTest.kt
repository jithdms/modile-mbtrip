package com.example.mbtrip.createtrip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class PickerMathTest {

    @Test fun 달력_칸은_일요일부터_7의_배수() {
        val nov = monthCells(YearMonth.of(2026, 11)) // 11/1 = 일요일
        assertEquals(LocalDate.of(2026, 11, 1), nov[0])
        assertEquals(0, nov.size % 7)
        val oct = monthCells(YearMonth.of(2026, 10)) // 10/1 = 목요일 → 앞 빈칸 4개
        assertNull(oct[3])
        assertEquals(LocalDate.of(2026, 10, 1), oct[4])
        assertEquals(0, oct.size % 7)
    }

    @Test fun 오전_오후_변환() {
        assertEquals(false to 12, to12h(0))
        assertEquals(false to 9, to12h(9))
        assertEquals(true to 12, to12h(12))
        assertEquals(true to 3, to12h(15))
        for (h in 0..23) {
            val (pm, h12) = to12h(h)
            assertEquals(h, from12h(pm, h12))
        }
    }

    @Test fun 기간_밖_날짜는_당겨오고_못고름() {
        val min = LocalDate.of(2026, 11, 7)
        val max = LocalDate.of(2026, 11, 8)
        assertEquals(min, clampDate(LocalDate.of(2026, 10, 1), min, max))
        assertEquals(max, clampDate(LocalDate.of(2026, 12, 1), min, max))
        assertTrue(!isSelectable(LocalDate.of(2026, 11, 9), min, max))
        assertTrue(isSelectable(LocalDate.of(2026, 11, 9), null, null))
    }

    @Test fun 미리보기_문구() {
        assertEquals("11월 7일 (토) 오후 3:05", previewText(LocalDateTime.of(2026, 11, 7, 15, 5)))
        assertEquals("10월 1일 (목) 오전 12:00", previewText(LocalDateTime.of(2026, 10, 1, 0, 0)))
    }
}
