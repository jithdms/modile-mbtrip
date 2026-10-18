package com.example.mbtrip.createtrip

import com.example.mbtrip.timeline.model.MarkerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TripSetupTest {

    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, d, h, m)
    private val ok = TripSetup(checkIn = at(7, 15), checkOut = at(8, 11))

    @Test fun 숙소가_있으면_체크인_체크아웃_필수() {
        assertEquals("체크인 시간을 입력해 주세요.", TripSetup().problem())
        assertEquals("체크아웃 시간을 입력해 주세요.", TripSetup(checkIn = at(7, 15)).problem())
        assertNull(ok.problem())
    }

    @Test fun 체크아웃은_체크인보다_늦어야_함() {
        assertEquals("체크아웃은 체크인보다 늦어야 해요.", ok.copy(checkOut = at(7, 14)).problem())
        assertEquals("체크아웃은 체크인보다 늦어야 해요.", ok.copy(checkOut = at(7, 15)).problem())
    }

    @Test fun 당일치기는_숙소_없이_저장되고_선도_없다() {
        val day = TripSetup(dayTrip = true, checkIn = at(7, 15)) // 예전에 적어둔 값은 무시
        assertNull(day.problem())
        assertTrue(day.toMarkers().isEmpty())
    }

    @Test fun 교통_도착이_출발보다_빠르면_안됨() {
        val bad = ok.copy(outbound = TransportLeg(at(7, 9), at(7, 8)))
        assertEquals("가는 편 도착 시간이 출발 시간보다 빨라요.", bad.problem())
        val back = ok.copy(outbound = TransportLeg(at(8, 9)), inbound = TransportLeg(at(7, 18)))
        assertEquals("오는 편이 가는 편보다 빨라요.", back.problem())
    }

    @Test fun 여행_기간_밖_날짜는_안됨() {
        val start = LocalDate.of(2026, 11, 7)
        val end = LocalDate.of(2026, 11, 8)
        assertNull(ok.problem(start, end))
        assertTrue(ok.copy(checkOut = at(9, 11)).problem(start, end)!!.startsWith("여행 기간"))
    }

    @Test fun 체크인_체크아웃이_일정표_선이_된다() {
        val m = ok.toMarkers()
        assertEquals(listOf(MarkerType.CHECK_IN, MarkerType.CHECK_OUT), m.map { it.type })
        assertEquals(LocalDate.of(2026, 11, 7), m[0].date)
        assertEquals(11, m[1].time.hour)
    }

    @Test fun 날짜_범위는_가장_이른날부터_늦은날까지() {
        val s = ok.copy(inbound = TransportLeg(at(9, 18), at(9, 20)))
        assertEquals(LocalDate.of(2026, 11, 7) to LocalDate.of(2026, 11, 9), s.dateRange())
        assertNull(TripSetup(dayTrip = true).dateRange())
    }

    @Test fun 화면_표시는_오전_오후() {
        assertEquals("2026.10.01  오전 10:10", LocalDateTime.of(2026, 10, 1, 10, 10).displayText())
        assertEquals("2026.11.07  오후 3:00", at(7, 15).displayText())
    }
}
