package com.example.mbtrip.timeline.ui

import com.example.mbtrip.timeline.model.Block
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Compose 에 의존하지 않는 순수 로직 — JUnit 으로 바로 테스트 가능.
 * 2주차에 ViewModel 로 옮길 때 그대로 가져간다.
 */

internal val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun Block.timeText() = "${start.format(HM)} ~ ${end.format(HM)}"

/** LazyColumn 한 줄 = 블록 또는 '현재 시각' 선 */
internal sealed interface TimelineItem {
    data class BlockItem(val block: Block) : TimelineItem
    data object NowItem : TimelineItem
}

/** 블록 목록 사이에 '현재 시각' 선을 끼워 넣는다. 그날 일정 범위 밖이면 넣지 않는다. */
internal fun buildItems(blocks: List<Block>, now: LocalTime?): List<TimelineItem> {
    val items = blocks.map<Block, TimelineItem> { TimelineItem.BlockItem(it) }.toMutableList()
    if (now != null && blocks.isNotEmpty() && now >= blocks.first().start && now <= blocks.last().end) {
        val idx = blocks.indexOfFirst { it.start > now }.let { if (it == -1) blocks.size else it }
        items.add(idx, TimelineItem.NowItem)
    }
    return items
}

/** 상세 시트 저장. 시간이 "15:30 ~ 16:30" 형식이 아니거나 시작≥종료면 기존 시간 유지. version + 1. */
internal fun Block.applyEdit(tags: List<String>, place: String, time: String, memo: String): Block {
    val parts = time.split("~").map { it.trim() }
    val parsed = runCatching { LocalTime.parse(parts[0]) to LocalTime.parse(parts[1]) }.getOrNull()
    val (ns, ne) = parsed?.takeIf { it.first < it.second } ?: (start to end)
    return copy(
        tags = tags, place = place.ifBlank { null }, memo = memo.ifBlank { null },
        start = ns, end = ne, version = version + 1,
    )
}
