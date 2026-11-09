package com.example.mbtrip.createtrip

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * 날짜·시간 선택 시트의 계산만 모아 둔 파일 (화면 없이 테스트 가능).
 */

/**
 * 달력 한 달 칸: 일요일부터 시작, 앞쪽 빈칸은 null, 7의 배수로 맞춤.
 * 예) 2026년 11월 1일은 일요일 → 빈칸 없이 1일부터
 */
fun monthCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value % 7 // 일=0, 월=1 … 토=6
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(lead) { null } + days
    val tail = (7 - cells.size % 7) % 7
    return cells + List<LocalDate?>(tail) { null }
}

/** 24시간 → (오후인가, 1~12시) */
fun to12h(hour24: Int): Pair<Boolean, Int> {
    val pm = hour24 >= 12
    val h = hour24 % 12
    return pm to (if (h == 0) 12 else h)
}

/** (오후인가, 1~12시) → 24시간 */
fun from12h(pm: Boolean, hour12: Int): Int {
    val h = hour12 % 12 // 12시 → 0
    return if (pm) h + 12 else h
}

/** 고를 수 없는 날짜면 기간 안으로 당긴다 */
fun clampDate(d: LocalDate, min: LocalDate?, max: LocalDate?): LocalDate = when {
    min != null && d < min -> min
    max != null && d > max -> max
    else -> d
}

fun isSelectable(d: LocalDate, min: LocalDate?, max: LocalDate?): Boolean =
    (min == null || d >= min) && (max == null || d <= max)

/** 미리보기 문구: 11월 7일 (토) 오후 3:05 */
fun previewText(dt: LocalDateTime): String {
    val dow = dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val (pm, h) = to12h(dt.hour)
    return "${dt.monthValue}월 ${dt.dayOfMonth}일 ($dow) ${if (pm) "오후" else "오전"} $h:${"%02d".format(dt.minute)}"
}

val LocalDate.isSunday get() = dayOfWeek == DayOfWeek.SUNDAY
val LocalDate.isSaturday get() = dayOfWeek == DayOfWeek.SATURDAY
