package com.example.mbtrip.timeline.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * 일정 블록 (Firestore 컬렉션: Block) — 제안서 부록 A2 기준.
 *
 * A2 대비 변경점 (데이터 구조 변경 → PR 표시 + 디스코드 공지 대상):
 *  - kind 에 MOVE(이동) 추가        : 와이어프레임의 검정 블록
 *  - tags 추가 (#맛집, #명소 등)     : 와이어프레임 블록 상세 모달
 *  - memo 추가                       : 와이어프레임 블록 상세 모달
 *
 * 1주차에는 java.time 으로 두고, 2주차 Firestore 연결 시 Timestamp ↔ 변환 함수만 추가한다.
 */
data class Block(
    val id: String,
    val tripId: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val kind: BlockKind,
    val title: String,
    val place: String? = null,
    val status: BlockStatus,
    val source: BlockSource,
    val locked: Boolean = false,
    val version: Int = 1,
    val tags: List<String> = emptyList(),
    val memo: String? = null,
) {
    init {
        require(start < end) { "Block($id): start($start) 는 end($end) 보다 빨라야 합니다." }
    }

    /** 화면 색을 결정하는 단일 규칙 — UI는 이 값만 보고 색을 고른다. */
    val display: BlockDisplay
        get() = when {
            kind == BlockKind.MOVE -> BlockDisplay.MOVE          // 검정: 기본 이동 시간
            status != BlockStatus.CONFIRMED -> BlockDisplay.UNDECIDED // 회색: 미정(제안됨/투표중)
            kind == BlockKind.FIXED -> BlockDisplay.FIXED        // 하늘색: 확정 블록
            else -> BlockDisplay.FREE                            // 초록: 자유 블록
        }
}

/** A2 '종류(확정/자유)' + 이동 */
enum class BlockKind { MOVE, FIXED, FREE }

/** A2 '상태' */
enum class BlockStatus { PROPOSED, VOTING, CONFIRMED }

/** A2 '출처' */
enum class BlockSource { USER, AUTO, TIDE, SUNSET, RECOMMEND }

/** 화면 표시 분류 (저장하지 않음, 계산값) */
enum class BlockDisplay { MOVE, FIXED, FREE, UNDECIDED }

/** 동행자 (A2 Member 중 화면에 필요한 최소 필드) */
data class Member(
    val userId: String,
    val nickname: String,
    val handle: String,
    val jpScore: Int, // 0(P) ~ 100(J)
)
