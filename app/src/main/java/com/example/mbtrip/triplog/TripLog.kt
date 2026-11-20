package com.example.mbtrip.triplog

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 여행 모임 로그 1개 = 한 사람이 한 여행에 남긴 후기(Review) 1개.
 *
 *  저장 위치 (데이터 구조 문서 그대로)
 *   - trips/{tripId}                 destination · startDate · endDate  → 로그의 장소·기간
 *   - trips/{tripId}/reviews/{userId} id · tripId · authorId · satisfaction(1~5) · createdAt
 *       + 앱 확장 필드: title(제목) · content(본문) · photoUrls(사진 주소 목록)
 *   - trips/{tripId}/members/{userId} 함께한 사람 (상세 화면 오른쪽 아래 프로필)
 *
 *  사진 주소(photoUrls)는 "https://…" 인터넷 주소 또는 "res:photo1" 처럼 앱 안 그림 이름.
 */
data class TripLog(
    val tripId: String,
    val authorId: String,
    val title: String,
    val content: String,
    /** 만족도 1~5. 0 = 아직 평가 없음 */
    val satisfaction: Int,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val photos: List<String>,
    val createdAtMillis: Long = 0L,
    /** true = DB 에 없는 예시 로그 (화면 확인용) */
    val isSample: Boolean = false,
) {
    val key: String get() = "$tripId/$authorId"
}

/** 달력에서 고른 기간 (양 끝 포함) */
data class LogDateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "끝나는 날이 시작하는 날보다 빨라요." }
    }
}

/** 여행 기간이 고른 기간과 하루라도 겹치면 보여준다 */
fun TripLog.overlaps(range: LogDateRange): Boolean =
    !startDate.isAfter(range.end) && !endDate.isBefore(range.start)

/** 검색어(제목·본문·장소) + 기간으로 거르기. 빈 검색어·null 기간 = 거르지 않음 */
fun List<TripLog>.filterLogs(query: String, range: LogDateRange?): List<TripLog> {
    val q = query.trim().lowercase()
    return filter { log ->
        val matchesQuery = q.isEmpty() ||
            log.title.lowercase().contains(q) ||
            log.content.lowercase().contains(q) ||
            log.destination.lowercase().contains(q)
        matchesQuery && (range == null || log.overlaps(range))
    }
}

/* ───────────── 화면 글자 ───────────── */

private fun LocalDate.dot(): String = "%d.%02d.%02d".format(year, monthValue, dayOfMonth)
private fun LocalDate.dotShort(): String = "%02d.%02d".format(monthValue, dayOfMonth)

/** 2026.04.11 – 04.13 (해가 바뀌면 끝도 연도까지) */
fun formatDateRange(start: LocalDate, end: LocalDate): String = when {
    start == end -> start.dot()
    start.year == end.year -> "${start.dot()} – ${end.dotShort()}"
    else -> "${start.dot()} – ${end.dot()}"
}

fun formatDateRange(range: LogDateRange): String = formatDateRange(range.start, range.end)

/** 2박 3일 / 당일치기 */
fun nightsAndDays(start: LocalDate, end: LocalDate): String {
    val nights = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    return if (nights == 0) "당일치기" else "${nights}박 ${nights + 1}일"
}

/* ───────────── 사진 모아보기 칸 ───────────── */

sealed class GalleryItem {
    /** 달마다 맨 위 제목 줄 (두 칸을 다 차지) */
    data class Header(val label: String, val detail: String) : GalleryItem()

    /** 사진 1장. ref == null 이면 사진 없는 로그 (빈 카드) */
    data class Photo(
        val log: TripLog,
        val index: Int,
        val ref: String?,
        val isCover: Boolean,
        val heightDp: Int,
    ) : GalleryItem()
}

/** 사진 높이를 조금씩 다르게 → 두 줄이 엇갈리며 아래로 계속 이어진다 */
private val TILE_HEIGHTS_DP = intArrayOf(230, 170, 200, 250, 180, 220)

/** 로그 목록(최근 여행 먼저) → 달별 제목 + 사진 칸 */
fun buildGalleryItems(logs: List<TripLog>): List<GalleryItem> {
    val out = mutableListOf<GalleryItem>()
    var tile = 0
    logs.groupBy { YearMonth.from(it.startDate) }.forEach { (month, group) ->
        val photoCount = group.sumOf { it.photos.size }
        out += GalleryItem.Header("${month.year}년 ${month.monthValue}월", "로그 ${group.size}개 · 사진 ${photoCount}장")
        group.forEach { log ->
            val refs: List<String?> = log.photos.ifEmpty { listOf(null) }
            refs.forEachIndexed { i, ref ->
                out += GalleryItem.Photo(log, i, ref, isCover = i == 0, heightDp = TILE_HEIGHTS_DP[tile++ % TILE_HEIGHTS_DP.size])
            }
        }
    }
    return out
}

/** 최근 여행 먼저, 같은 여행이면 최근에 쓴 글 먼저 */
fun List<TripLog>.sortedForGallery(): List<TripLog> =
    sortedWith(compareByDescending<TripLog> { it.startDate }.thenByDescending { it.createdAtMillis })
