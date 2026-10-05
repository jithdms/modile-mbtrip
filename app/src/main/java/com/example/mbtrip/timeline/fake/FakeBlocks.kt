package com.example.mbtrip.timeline.fake

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind.FIXED
import com.example.mbtrip.timeline.model.BlockKind.FREE
import com.example.mbtrip.timeline.model.BlockKind.MOVE
import com.example.mbtrip.timeline.model.BlockSource.AUTO
import com.example.mbtrip.timeline.model.BlockSource.RECOMMEND
import com.example.mbtrip.timeline.model.BlockSource.SUNSET
import com.example.mbtrip.timeline.model.BlockSource.TIDE
import com.example.mbtrip.timeline.model.BlockSource.USER
import com.example.mbtrip.timeline.model.BlockStatus.CONFIRMED
import com.example.mbtrip.timeline.model.BlockStatus.PROPOSED
import com.example.mbtrip.timeline.model.BlockStatus.VOTING
import com.example.mbtrip.timeline.model.Member
import java.time.LocalDate
import java.time.LocalTime

/**
 * 1주차용 가짜 데이터 — 제안서 스토리보드
 * "J 민지(82점)와 P 준호(28점)의 시흥 거북섬 1박" 시나리오.
 *
 * ⚠ 간조·일몰 시각, 식당 이름은 모두 예시값이다.
 *   2주차에 조석예보(국립해양조사원)·출몰시각(한국천문연구원) API와 팀 조사 상권 데이터로 교체한다.
 *
 * 구성 원칙: 상태(PROPOSED/VOTING/CONFIRMED), 출처(USER/AUTO/TIDE/SUNSET/RECOMMEND),
 * 표시색(검정/하늘/초록/회색)이 모두 한 번 이상 등장 → 팀원 각자 화면에서 모든 경우 테스트 가능.
 */
object FakeBlocks {

    const val TRIP_ID = "trip_fake_geobukseom"
    val DAY1: LocalDate = LocalDate.of(2026, 11, 7)
    val DAY2: LocalDate = LocalDate.of(2026, 11, 8)

    val members = listOf(
        Member("u_minji", "민지", "@minji_j", jpScore = 82),
        Member("u_junho", "준호", "@junho_p", jpScore = 28),
        Member("u_seoyeon", "서연", "@seoyeon", jpScore = 55), // 와이어프레임 아바타 3개에 맞춘 가상 인물
    )

    val blocks: List<Block> = listOf(
        // ───────── Day 1 ─────────
        b("b01", DAY1, "08:30", "09:40", MOVE, "기차로 이동", "서울 → 시흥", CONFIRMED, USER, locked = true),
        b("b02", DAY1, "09:40", "10:20", MOVE, "버스로 거북섬 이동", "시흥 → 거북섬", CONFIRMED, USER, locked = true),
        b("b03", DAY1, "10:30", "12:30", FIXED, "웨이브파크 서핑", "시흥 웨이브파크", CONFIRMED, USER,
            locked = true, tags = listOf("명소")),
        b("b04", DAY1, "12:30", "13:30", FREE, "점심 후보 3곳", "거북섬 상권", PROPOSED, RECOMMEND,
            tags = listOf("맛집"), memo = "후보: 식당A / 식당B / 식당C (예시 — 팀 조사 데이터로 교체)"),
        b("b05", DAY1, "13:30", "14:20", FREE, "카페에서 쉬기", "거북섬", CONFIRMED, AUTO, tags = listOf("카페")),
        b("b06", DAY1, "14:30", "16:00", FIXED, "갯벌 체험 (간조)", "오이도 갯벌", VOTING, TIDE,
            tags = listOf("명소"), memo = "간조 시각 예시값. 준호 반대 사유: \"피곤할 듯\" → 대체안: 배곧 산책"),
        b("b07", DAY1, "16:00", "17:00", FREE, "자유 시간", null, CONFIRMED, AUTO),
        b("b08", DAY1, "17:10", "17:50", FIXED, "일몰 보기", "거북섬 해안", CONFIRMED, SUNSET,
            locked = true, memo = "일몰 시각 예시값"),
        b("b09", DAY1, "18:00", "19:30", FREE, "저녁 후보", "거북섬 상권", PROPOSED, RECOMMEND,
            tags = listOf("맛집"), memo = "제휴 쿠폰 표시 예정 (S2)"),
        b("b10", DAY1, "19:40", "20:00", MOVE, "숙소로 이동", "거북섬 → 숙소", CONFIRMED, AUTO),
        b("b11", DAY1, "20:00", "20:30", FIXED, "숙소 체크인", "거북섬 숙소", CONFIRMED, USER, locked = true),

        // ───────── Day 2 ─────────
        b("b12", DAY2, "09:00", "10:00", FREE, "브런치", "거북섬 상권", VOTING, RECOMMEND, tags = listOf("맛집")),
        b("b13", DAY2, "10:00", "10:30", FIXED, "체크아웃", "거북섬 숙소", CONFIRMED, USER, locked = true),
        b("b14", DAY2, "10:40", "12:00", FREE, "거북섬 산책", "거북섬", CONFIRMED, AUTO, tags = listOf("명소")),
        b("b15", DAY2, "12:10", "13:20", FIXED, "점심 (예약)", "거북섬 상권", CONFIRMED, USER, tags = listOf("맛집")),
        b("b16", DAY2, "14:00", "15:20", MOVE, "기차로 귀가", "시흥 → 서울", CONFIRMED, USER, locked = true),
    )

    /** 날짜별로 묶고 시작 시각 순 정렬 — TimelineScreen 이 그대로 사용 */
    val byDate: Map<LocalDate, List<Block>> =
        blocks.groupBy { it.date }.toSortedMap().mapValues { (_, v) -> v.sortedBy { it.start } }

    private fun b(
        id: String, date: LocalDate, start: String, end: String,
        kind: com.example.mbtrip.timeline.model.BlockKind, title: String, place: String?,
        status: com.example.mbtrip.timeline.model.BlockStatus, source: com.example.mbtrip.timeline.model.BlockSource,
        locked: Boolean = false, tags: List<String> = emptyList(), memo: String? = null,
    ) = Block(
        id = id, tripId = TRIP_ID, date = date,
        start = LocalTime.parse(start), end = LocalTime.parse(end),
        kind = kind, title = title, place = place, status = status, source = source,
        locked = locked, tags = tags, memo = memo,
    )
}
