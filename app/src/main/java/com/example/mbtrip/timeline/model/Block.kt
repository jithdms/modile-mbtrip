package com.example.mbtrip.timeline.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * 일정 블록 (Firestore 컬렉션: Block) — 제안서 부록 A2 기준.
 *
 * A2 대비 변경점 (데이터 구조 변경 → PR 표시 + 디스코드 공지 대상):
 *  - kind 에 MOVE(이동) 추가
 *  - title, tags, memo 추가
 *  - todo 추가        : 확정 블록의 '할 일' (마크다운 글)
 *  - transport 추가   : 이동 블록의 이동수단
 *
 * 종류별 규칙 (init 에서 강제):
 *  - 이동(MOVE)  : 항상 확정됨. 시간·이동수단·메모만 사용
 *  - 자유(FREE)  : 항상 확정됨(제안됨 없음). 메모만 사용. 메모가 비면 '빈칸'
 *  - 확정(FIXED) : 제안됨 / 투표중 / 확정됨. 제목·시간·장소·할 일·메모·태그 사용
 */
data class Block(
    val id: String,
    val tripId: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val kind: BlockKind,
    val status: BlockStatus,
    val source: BlockSource,
    val title: String = "",
    val place: String? = null,
    val locked: Boolean = false,
    val version: Int = 1,
    val tags: List<String> = emptyList(),
    val memo: String? = null,
    val todo: String = "",
    val transport: Transport? = null,
) {
    init {
        require(start < end) { "Block($id): start($start) 는 end($end) 보다 빨라야 합니다." }
        require(kind == BlockKind.FIXED || status == BlockStatus.CONFIRMED) {
            "Block($id): 이동·자유 블록은 항상 확정됨 상태여야 합니다."
        }
    }

    /** 자유 블록인데 메모가 비어 있으면 '빈칸' */
    val isEmptyFree: Boolean get() = kind == BlockKind.FREE && memo.isNullOrBlank()

    /** 화면 색을 결정하는 단일 규칙 — UI는 이 값만 보고 색을 고른다. */
    val display: BlockDisplay
        get() = when (kind) {
            BlockKind.MOVE -> BlockDisplay.MOVE
            BlockKind.FREE -> if (isEmptyFree) BlockDisplay.FREE_EMPTY else BlockDisplay.FREE
            BlockKind.FIXED ->
                if (status == BlockStatus.CONFIRMED) BlockDisplay.FIXED else BlockDisplay.FIXED_PENDING
        }

    /** 카드에 보여줄 제목 */
    val displayTitle: String
        get() = when (kind) {
            BlockKind.MOVE -> transport?.let { "${it.label}${it.particle} 이동" } ?: title.ifBlank { "이동" }
            BlockKind.FREE -> if (isEmptyFree) "빈칸 · 눌러서 채우기" else memo!!.lineSequence().first().trim()
            BlockKind.FIXED -> title.ifBlank { "제목 없음" }
        }
}

/** A2 '종류(확정/자유)' + 이동 */
enum class BlockKind { MOVE, FIXED, FREE }

/** A2 '상태' — 확정(FIXED) 블록만 PROPOSED / VOTING 가능 */
enum class BlockStatus { PROPOSED, VOTING, CONFIRMED }

/** A2 '출처' */
enum class BlockSource { USER, AUTO, TIDE, SUNSET, RECOMMEND }

/** 화면 표시 분류 (저장하지 않음, 계산값) */
enum class BlockDisplay {
    MOVE,          // 검정, 실선
    FIXED,         // 하늘, 실선
    FIXED_PENDING, // 연한 하늘, 점선 (제안됨·투표중)
    FREE,          // 초록, 실선
    FREE_EMPTY,    // 연한 초록, 점선 (빈칸)
}

/** 이동수단 — particle 은 '로/으로' 조사 */
enum class Transport(val label: String, val particle: String) {
    TRAIN("기차", "로"),
    BUS("버스", "로"),
    SUBWAY("지하철", "로"),
    TAXI("택시", "로"),
    CAR("자가용", "으로"),
    WALK("도보", "로"),
    PLANE("비행기", "로"),
}

/**
 * 타임라인 위에 선으로만 표시하는 시각 (블록 아님).
 * 숙소 체크인·체크아웃 — 와이어프레임 '방 생성 페이지1'(숙소 입력)에서 들어올 값.
 */
data class TimeMarker(
    val date: LocalDate,
    val time: LocalTime,
    val type: MarkerType,
) {
    val label: String get() = type.label
}

/** 체크인 = 오른쪽 화살표(→), 체크아웃 = 왼쪽 화살표(←) */
enum class MarkerType(val label: String) { CHECK_IN("체크인"), CHECK_OUT("체크아웃") }

/** 동행자 (A2 Member 중 화면에 필요한 최소 필드) */
data class Member(
    val userId: String,
    val nickname: String,
    val handle: String,
    val jpScore: Int, // 0(J) ~ 100(P), Firestore Member convention
)
