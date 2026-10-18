package com.example.mbtrip.timeline.data

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Transport
import java.time.LocalDate
import java.time.LocalTime

/*
 * Block ↔ Firestore 문서(Map) 변환 — Firebase 라이브러리 없이 동작하는 순수 함수라 JUnit 으로 테스트한다.
 *
 * 저장 위치: trips/{tripId}/blocks/{blockId}
 * 날짜·시간은 글자로 저장 (콘솔에서 바로 읽히고 시간대 문제가 없음)
 *   date  = "2026-11-07"
 *   start = "08:30", end = "09:40"
 * enum 은 이름 그대로 ("FIXED", "PROPOSED" …)
 */

object BlockFields {
    const val DATE = "date"
    const val START = "start"
    const val END = "end"
    const val KIND = "kind"
    const val STATUS = "status"
    const val SOURCE = "source"
    const val TITLE = "title"
    const val PLACE = "place"
    const val LOCKED = "locked"
    const val VERSION = "version"
    const val TAGS = "tags"
    const val MEMO = "memo"
    const val TODO = "todo"
    const val TRANSPORT = "transport"
    const val UPDATED_AT = "updatedAt" // FirestoreBlockRepository 가 서버 시각으로 채움
}

/** Block → Firestore 에 쓸 Map (id·tripId 는 문서 경로에 들어가므로 넣지 않음) */
fun Block.toFirestoreMap(): Map<String, Any?> = mapOf(
    BlockFields.DATE to date.toString(),
    BlockFields.START to start.toString(),   // LocalTime.toString() → "08:30"
    BlockFields.END to end.toString(),
    BlockFields.KIND to kind.name,
    BlockFields.STATUS to status.name,
    BlockFields.SOURCE to source.name,
    BlockFields.TITLE to title,
    BlockFields.PLACE to place,
    BlockFields.LOCKED to locked,
    BlockFields.VERSION to version,
    BlockFields.TAGS to tags,
    BlockFields.MEMO to memo,
    BlockFields.TODO to todo,
    BlockFields.TRANSPORT to transport?.name,
)

/**
 * Firestore 문서 → Block. 필수 값이 없거나 형식이 틀리면 null (앱이 죽지 않고 그 블록만 건너뜀).
 * Firestore 는 숫자를 Long 으로 돌려주므로 version 은 Number 로 받는다.
 */
fun blockFromFirestore(id: String, tripId: String, data: Map<String, Any?>): Block? = runCatching {
    Block(
        id = id,
        tripId = tripId,
        date = LocalDate.parse(data[BlockFields.DATE] as String),
        start = LocalTime.parse(data[BlockFields.START] as String),
        end = LocalTime.parse(data[BlockFields.END] as String),
        kind = BlockKind.valueOf(data[BlockFields.KIND] as String),
        status = BlockStatus.valueOf(data[BlockFields.STATUS] as String),
        source = (data[BlockFields.SOURCE] as? String)?.let { BlockSource.valueOf(it) } ?: BlockSource.USER,
        title = data[BlockFields.TITLE] as? String ?: "",
        place = data[BlockFields.PLACE] as? String,
        locked = data[BlockFields.LOCKED] as? Boolean ?: false,
        version = (data[BlockFields.VERSION] as? Number)?.toInt() ?: 1,
        tags = (data[BlockFields.TAGS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        memo = data[BlockFields.MEMO] as? String,
        todo = data[BlockFields.TODO] as? String ?: "",
        transport = (data[BlockFields.TRANSPORT] as? String)?.let { Transport.valueOf(it) },
    )
}.getOrNull()
