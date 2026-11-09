package com.example.mbtrip.createtrip

import com.example.mbtrip.timeline.model.MarkerType
import com.example.mbtrip.timeline.model.TimeMarker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 교통 한 편 (가는 편 또는 오는 편) */
data class TransportLeg(
    val departure: LocalDateTime? = null,
    val arrival: LocalDateTime? = null,
)

/** 가는 편 = 탭 '출발', 오는 편 = 탭 '도착' */
enum class LegDirection(val label: String) { OUTBOUND("출발"), INBOUND("도착") }

/**
 * 방 만들기 다음 화면(숙소·교통)에서 입력한 값.
 * 당일치기면 체크인·체크아웃은 무시한다.
 */
data class TripSetup(
    val dayTrip: Boolean = false,
    val checkIn: LocalDateTime? = null,
    val checkOut: LocalDateTime? = null,
    val outbound: TransportLeg = TransportLeg(),
    val inbound: TransportLeg = TransportLeg(),
) {
    fun leg(dir: LegDirection): TransportLeg = if (dir == LegDirection.OUTBOUND) outbound else inbound

    fun withLeg(dir: LegDirection, leg: TransportLeg): TripSetup =
        if (dir == LegDirection.OUTBOUND) copy(outbound = leg) else copy(inbound = leg)
}

/**
 * 저장할 수 없는 이유. null 이면 저장 가능.
 * @param tripStart, tripEnd 방 만들기에서 고른 여행 기간 (아직 넘겨받지 못하면 null → 기간 검사 생략)
 */
fun TripSetup.problem(tripStart: LocalDate? = null, tripEnd: LocalDate? = null): String? {
    if (!dayTrip) {
        val inT = checkIn ?: return "체크인 시간을 입력해 주세요."
        val outT = checkOut ?: return "체크아웃 시간을 입력해 주세요."
        if (!outT.isAfter(inT)) return "체크아웃은 체크인보다 늦어야 해요."
    }
    legProblem(outbound, "가는 편")?.let { return it }
    legProblem(inbound, "오는 편")?.let { return it }
    val (o, i) = outbound.departure to inbound.departure
    if (o != null && i != null && i.isBefore(o)) return "오는 편이 가는 편보다 빨라요."

    if (tripStart != null && tripEnd != null) {
        val all = buildList {
            if (!dayTrip) { add(checkIn); add(checkOut) }
            add(outbound.departure); add(outbound.arrival); add(inbound.departure); add(inbound.arrival)
        }.filterNotNull()
        if (all.any { it.toLocalDate() < tripStart || it.toLocalDate() > tripEnd }) {
            return "여행 기간(${tripStart.format(DATE)} ~ ${tripEnd.format(DATE)}) 안의 날짜를 골라 주세요."
        }
    }
    return null
}

private fun legProblem(leg: TransportLeg, name: String): String? {
    val d = leg.departure ?: return null
    val a = leg.arrival ?: return null
    return if (!a.isAfter(d)) "$name 도착 시간이 출발 시간보다 빨라요." else null
}

/** 일정표의 파란 선 (체크인 →, 체크아웃 ←) */
fun TripSetup.toMarkers(): List<TimeMarker> {
    if (dayTrip) return emptyList()
    return listOfNotNull(
        checkIn?.let { TimeMarker(it.toLocalDate(), it.toLocalTime(), MarkerType.CHECK_IN) },
        checkOut?.let { TimeMarker(it.toLocalDate(), it.toLocalTime(), MarkerType.CHECK_OUT) },
    )
}

/** 입력한 날짜들 중 가장 이른 날 ~ 가장 늦은 날 (일정표 날짜 탭 범위). 아무것도 없으면 null */
fun TripSetup.dateRange(): Pair<LocalDate, LocalDate>? {
    val dates = buildList {
        if (!dayTrip) { add(checkIn); add(checkOut) }
        add(outbound.departure); add(outbound.arrival); add(inbound.departure); add(inbound.arrival)
    }.filterNotNull().map { it.toLocalDate() }
    if (dates.isEmpty()) return null
    return dates.min() to dates.max()
}

/** 화면 표시용: 2026.10.01  오전 10:10 */
fun LocalDateTime.displayText(): String =
    "${format(DATE)}  ${format(TIME_KO)}"

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val TIME_KO: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/**
 * 화면 사이에 값을 넘기는 임시 보관함 (Firestore 저장은 10/22에 붙임).
 * 숙소·교통 화면 → 저장하기 → 일정표 가 여기서 꺼내 쓴다.
 */
object TripSetupStore {
    var current: TripSetup? = null
}
