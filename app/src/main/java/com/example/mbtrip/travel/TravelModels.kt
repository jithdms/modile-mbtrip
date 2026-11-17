package com.example.mbtrip.travel

import com.example.mbtrip.timeline.model.Member
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class TripInput(val departure: String, val destination: String, val start: LocalDate, val end: LocalDate, val headcount: Int) {
    init {
        require(departure.isNotBlank() && destination.isNotBlank()) { "출발지와 도착지를 입력해주세요." }
        require(!end.isBefore(start)) { "종료일이 시작일보다 빨라요." }
        require(java.time.temporal.ChronoUnit.DAYS.between(start, end) < 31) { "여행 기간은 최대 31일이에요." }
        require(headcount in 1..50) { "여행 인원은 1~50명으로 입력해주세요." }
    }
    companion object {
        fun parse(departure: String, destination: String, dates: String, headcount: String): TripInput {
            val parts = dates.split("~").map { it.trim() }
            require(parts.size == 2) { "날짜를 2026.11.17 ~ 2026.11.18 형식으로 입력해주세요." }
            val format = DateTimeFormatter.ofPattern("uuuu.MM.dd").withResolverStyle(ResolverStyle.STRICT)
            val start = runCatching { LocalDate.parse(parts[0], format) }.getOrElse { throw IllegalArgumentException("시작 날짜를 확인해주세요.") }
            val end = runCatching { LocalDate.parse(parts[1], format) }.getOrElse { throw IllegalArgumentException("종료 날짜를 확인해주세요.") }
            val count = headcount.trim().removeSuffix("명").trim().toIntOrNull()
                ?: throw IllegalArgumentException("여행 인원을 숫자로 입력해주세요.")
            return TripInput(departure.trim(), destination.trim(), start, end, count)
        }
    }
}

data class ActiveTrip(val id: String, val ownerId: String?, val input: TripInput) {
    init { require(id.isNotBlank() && '/' !in id && (ownerId == null || ownerId.isNotBlank())) }
    fun isTraveling(today: LocalDate = LocalDate.now()) = today in input.start..input.end
}

object MemberFields {
    fun decode(id: String, data: Map<String, Any?>, traveling: Boolean): Member {
        val score = (data["jpScore"] as? Number)?.toInt()?.takeIf { it in 0..100 }
        return Member(
            userId = id,
            nickname = (data["nickname"] as? String)?.takeIf { it.isNotBlank() } ?: id,
            handle = (data["handle"] as? String)?.takeIf { it.isNotBlank() } ?: "@$id",
            jpScore = score ?: 50,
            isTraveling = traveling,
            photoUrl = (data["photoUrl"] as? String)?.takeIf { it.startsWith("https://") },
            hasDiagnosis = score != null,
        )
    }
}
