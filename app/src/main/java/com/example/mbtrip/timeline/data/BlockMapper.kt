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
 * ★ 팀 데이터 구조 문서(MBTrip 데이터 구조.pdf)의 Block 표를 그대로 따른다 ★
 *   경로   trips/{tripId}/blocks/{blockId}   (blockId = 문서 ID)
 *   tripId     string  "trip_001"            경로와 같은 값
 *   date       string  "2026-10-24"
 *   startTime  string  "10:00"
 *   endTime    string  "12:00"
 *   type       "fixed" | "free"              (+ 우리 앱 확장: "move" = 이동 블록)
 *   place      map {name, lat, lng}          좌표가 없으면 lat·lng 없이 name 만
 *   status     "proposed" | "voting" | "confirmed"
 *   source     "user" | "auto" | "siheung"
 *
 * 문서 표에 없는 '앱 확장 필드' (문서에 추가 요청할 것):
 *   title, tags, memo, todo, address, transport, locked, version, deleteRequested,
 *   autoKind("tide"|"sunset" — source=auto 의 세부 종류), updatedAt
 *
 * 읽을 때는 예전 형식(start/end, kind, 대문자 enum, place 글자 + lat/lng)도 읽어서
 * 이미 저장된 데이터가 사라지지 않는다. 다음에 저장하면 새 형식으로 바뀐다.
 */

object BlockFields {
    // ── 데이터 구조 문서의 필드
    const val TRIP_ID = "tripId"
    const val DATE = "date"
    const val START = "startTime"
    const val END = "endTime"
    const val TYPE = "type"
    const val PLACE = "place"          // map {name, lat, lng}
    const val PLACE_NAME = "name"
    const val PLACE_LAT = "lat"
    const val PLACE_LNG = "lng"
    const val STATUS = "status"
    const val SOURCE = "source"

    // ── 앱 확장 필드
    const val TITLE = "title"
    const val TAGS = "tags"
    const val MEMO = "memo"
    const val TODO = "todo"
    const val ADDRESS = "address"
    const val TRANSPORT = "transport"
    const val LOCKED = "locked"
    const val VERSION = "version"
    const val DELETE_REQUESTED = "deleteRequested"
    const val AUTO_KIND = "autoKind"
    const val UPDATED_AT = "updatedAt" // FirestoreBlockRepository 가 서버 시각으로 채움

    // ── 예전 형식 (읽기 전용)
    internal const val OLD_START = "start"
    internal const val OLD_END = "end"
    internal const val OLD_KIND = "kind"
    internal const val OLD_LAT = "lat"
    internal const val OLD_LNG = "lng"
}

/* ───────── enum ↔ 문서 값 ───────── */

internal fun BlockKind.toDoc(): String = when (this) {
    BlockKind.FIXED -> "fixed"
    BlockKind.FREE -> "free"
    BlockKind.MOVE -> "move"
}

internal fun blockKindFromDoc(v: String): BlockKind = when (v.lowercase()) {
    "fixed" -> BlockKind.FIXED
    "free" -> BlockKind.FREE
    "move" -> BlockKind.MOVE
    else -> error("알 수 없는 type: $v")
}

internal fun BlockStatus.toDoc(): String = name.lowercase() // proposed / voting / confirmed

internal fun blockStatusFromDoc(v: String): BlockStatus = BlockStatus.valueOf(v.uppercase())

/** 문서 값 source 와 세부 종류 autoKind */
internal fun BlockSource.toDoc(): Pair<String, String?> = when (this) {
    BlockSource.USER -> "user" to null
    BlockSource.AUTO -> "auto" to null
    BlockSource.TIDE -> "auto" to "tide"
    BlockSource.SUNSET -> "auto" to "sunset"
    BlockSource.RECOMMEND -> "siheung" to null
}

internal fun blockSourceFromDoc(source: String?, autoKind: String?): BlockSource = when (source?.lowercase()) {
    null, "", "user" -> BlockSource.USER
    "siheung", "recommend" -> BlockSource.RECOMMEND
    "auto" -> when (autoKind?.lowercase()) {
        "tide" -> BlockSource.TIDE
        "sunset" -> BlockSource.SUNSET
        else -> BlockSource.AUTO
    }
    "tide" -> BlockSource.TIDE       // 예전 형식 (대문자 이름)
    "sunset" -> BlockSource.SUNSET
    else -> BlockSource.USER
}

/** Block → Firestore 에 쓸 Map (id 는 문서 ID 라서 넣지 않음) */
fun Block.toFirestoreMap(): Map<String, Any?> {
    val (src, autoKind) = source.toDoc()
    val placeMap: Map<String, Any?>? =
        if (place.isNullOrBlank() && lat == null && lng == null) {
            null
        } else {
            buildMap {
                put(BlockFields.PLACE_NAME, place.orEmpty())
                if (lat != null && lng != null) {
                    put(BlockFields.PLACE_LAT, lat)
                    put(BlockFields.PLACE_LNG, lng)
                }
            }
        }
    return mapOf(
        BlockFields.TRIP_ID to tripId,
        BlockFields.DATE to date.toString(),
        BlockFields.START to start.toString(),   // LocalTime.toString() → "08:30"
        BlockFields.END to end.toString(),
        BlockFields.TYPE to kind.toDoc(),
        BlockFields.PLACE to placeMap,
        BlockFields.STATUS to status.toDoc(),
        BlockFields.SOURCE to src,
        BlockFields.AUTO_KIND to autoKind,
        BlockFields.TITLE to title,
        BlockFields.TAGS to tags,
        BlockFields.MEMO to memo,
        BlockFields.TODO to todo,
        BlockFields.ADDRESS to address,
        BlockFields.TRANSPORT to transport?.name,
        BlockFields.LOCKED to locked,
        BlockFields.VERSION to version,
        BlockFields.DELETE_REQUESTED to deleteRequested,
    )
}

/**
 * Firestore 문서 → Block. 필수 값이 없거나 형식이 틀리면 null (앱이 죽지 않고 그 블록만 건너뜀).
 * Firestore 는 숫자를 Long/Double 로 돌려주므로 Number 로 받는다.
 */
fun blockFromFirestore(id: String, tripId: String, data: Map<String, Any?>): Block? = runCatching {
    val startText = (data[BlockFields.START] ?: data[BlockFields.OLD_START]) as String
    val endText = (data[BlockFields.END] ?: data[BlockFields.OLD_END]) as String
    val typeText = (data[BlockFields.TYPE] ?: data[BlockFields.OLD_KIND]) as String

    // place: 새 형식 = map {name, lat, lng} / 예전 형식 = 글자 + 따로 있는 lat, lng
    val placeRaw = data[BlockFields.PLACE]
    val placeName: String?
    val lat: Double?
    val lng: Double?
    if (placeRaw is Map<*, *>) {
        placeName = (placeRaw[BlockFields.PLACE_NAME] as? String)?.takeIf { it.isNotBlank() }
        lat = (placeRaw[BlockFields.PLACE_LAT] as? Number)?.toDouble()
        lng = (placeRaw[BlockFields.PLACE_LNG] as? Number)?.toDouble()
    } else {
        placeName = placeRaw as? String
        lat = (data[BlockFields.OLD_LAT] as? Number)?.toDouble()
        lng = (data[BlockFields.OLD_LNG] as? Number)?.toDouble()
    }

    Block(
        id = id,
        tripId = tripId,
        date = LocalDate.parse(data[BlockFields.DATE] as String),
        start = LocalTime.parse(startText),
        end = LocalTime.parse(endText),
        kind = blockKindFromDoc(typeText),
        status = blockStatusFromDoc(data[BlockFields.STATUS] as String),
        source = blockSourceFromDoc(data[BlockFields.SOURCE] as? String, data[BlockFields.AUTO_KIND] as? String),
        title = data[BlockFields.TITLE] as? String ?: "",
        place = placeName,
        locked = data[BlockFields.LOCKED] as? Boolean ?: false,
        version = (data[BlockFields.VERSION] as? Number)?.toInt() ?: 1,
        tags = (data[BlockFields.TAGS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        memo = data[BlockFields.MEMO] as? String,
        todo = data[BlockFields.TODO] as? String ?: "",
        transport = (data[BlockFields.TRANSPORT] as? String)?.let { Transport.valueOf(it) },
        lat = lat,
        lng = lng,
        address = data[BlockFields.ADDRESS] as? String,
        deleteRequested = data[BlockFields.DELETE_REQUESTED] as? Boolean ?: false,
    )
}.getOrNull()
