package com.example.mbtrip.timeline.fake

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind.FIXED
import com.example.mbtrip.timeline.model.BlockKind.FREE
import com.example.mbtrip.timeline.model.BlockKind.MOVE
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockSource.AUTO
import com.example.mbtrip.timeline.model.BlockSource.RECOMMEND
import com.example.mbtrip.timeline.model.BlockSource.SUNSET
import com.example.mbtrip.timeline.model.BlockSource.TIDE
import com.example.mbtrip.timeline.model.BlockSource.USER
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.BlockStatus.CONFIRMED
import com.example.mbtrip.timeline.model.BlockStatus.PROPOSED
import com.example.mbtrip.timeline.model.BlockStatus.VOTING
import com.example.mbtrip.timeline.model.MarkerType
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.model.TimeMarker
import com.example.mbtrip.timeline.model.Transport
import java.time.LocalDate
import java.time.LocalTime

/**
 * 1주차용 가짜 데이터 — 제안서 스토리보드
 * "J 민지(82점)와 P 준호(28점)의 시흥 거북섬 1박" 시나리오.
 *
 * ⚠ 간조·일몰 시각, 식당 이름은 모두 예시값이다.
 *   2주차에 조석예보(국립해양조사원)·출몰시각(한국천문연구원) API와 팀 조사 상권 데이터로 교체한다.
 *
 * 구성 원칙: 상태 3종, 출처 5종, 표시 5종(이동·확정·미정·자유·빈칸)이 모두 한 번 이상 등장.
 */
object FakeBlocks {

    const val TRIP_ID = "trip_fake_geobukseom"
    val DAY1: LocalDate = LocalDate.of(2026, 11, 7)
    val DAY2: LocalDate = LocalDate.of(2026, 11, 8)

    /** 여행 기간 (블록이 없는 날도 탭에 나오도록 시작·종료일을 따로 둔다) */
    val TRIP_START: LocalDate = DAY1
    val TRIP_END: LocalDate = DAY2

    /** 숙소 체크인·체크아웃 — 블록이 아니라 파란 선으로 표시 (예시값) */
    val markers: List<TimeMarker> = listOf(
        TimeMarker(DAY1, LocalTime.of(20, 0), MarkerType.CHECK_IN),
        TimeMarker(DAY2, LocalTime.of(10, 0), MarkerType.CHECK_OUT),
    )

    val members = listOf(
        Member("u_minji", "민지", "@minji_j", jpScore = 18),
        Member("u_junho", "준호", "@junho_p", jpScore = 72),
        Member("u_seoyeon", "서연", "@seoyeon", jpScore = 55), // 와이어프레임 아바타 3개에 맞춘 가상 인물
    )

    private val SURF_TODO = """
        ## 준비물
        - [x] 수영복
        - [ ] 선크림
        - [ ] 수건
        - **10:20까지** 입장
    """.trimIndent()

    val blocks: List<Block> = listOf(
        // ───────── Day 1 ─────────
        move("b01", DAY1, "08:30", "09:40", Transport.TRAIN, locked = true, memo = "서울역 출발"),
        move("b02", DAY1, "09:40", "10:20", Transport.BUS, locked = true),
        fixed("b03", DAY1, "10:30", "12:30", "웨이브파크 서핑", "시흥 웨이브파크", CONFIRMED, USER,
            locked = true, tags = listOf("명소"), todo = SURF_TODO),
        free("b04", DAY1, "12:30", "13:30", memo = null),                       // 빈칸
        free("b05", DAY1, "13:30", "14:20", memo = "카페에서 쉬기"),
        fixed("b06", DAY1, "14:30", "16:00", "갯벌 체험 (간조)", "오이도 갯벌", VOTING, TIDE,
            tags = listOf("명소"), memo = "간조 시각 예시값. 준호 반대 사유: \"피곤할 듯\" → 대체안: 배곧 산책"),
        free("b07", DAY1, "16:00", "17:00", memo = "숙소 근처 산책 or 낮잠"),
        fixed("b08", DAY1, "17:10", "17:50", "일몰 보기", "거북섬 해안", CONFIRMED, SUNSET,
            locked = true, memo = "일몰 시각 예시값"),
        fixed("b09", DAY1, "18:00", "19:30", "저녁 후보", "거북섬 상권", PROPOSED, RECOMMEND,
            tags = listOf("맛집"), memo = "후보: 식당A / 식당B / 식당C (예시)"),
        move("b10", DAY1, "19:40", "20:00", Transport.TAXI, source = AUTO),

        // ───────── Day 2 ─────────
        fixed("b12", DAY2, "09:00", "10:00", "브런치", "거북섬 상권", VOTING, RECOMMEND, tags = listOf("맛집")),
        free("b14", DAY2, "10:40", "12:00", memo = ""),                          // 빈칸
        fixed("b15", DAY2, "12:10", "13:20", "점심 (예약)", "거북섬 상권", CONFIRMED, USER, tags = listOf("맛집")),
        move("b16", DAY2, "14:00", "15:20", Transport.TRAIN, locked = true),
    )

    private fun t(s: String): LocalTime = LocalTime.parse(s)

    private fun move(
        id: String, date: LocalDate, start: String, end: String, transport: Transport,
        locked: Boolean = false, memo: String? = null, source: BlockSource = USER,
    ) = Block(
        id = id, tripId = TRIP_ID, date = date, start = t(start), end = t(end),
        kind = MOVE, status = CONFIRMED, source = source,
        transport = transport, locked = locked, memo = memo,
    )

    private fun free(id: String, date: LocalDate, start: String, end: String, memo: String?) = Block(
        id = id, tripId = TRIP_ID, date = date, start = t(start), end = t(end),
        kind = FREE, status = CONFIRMED, source = AUTO, memo = memo,
    )

    private fun fixed(
        id: String, date: LocalDate, start: String, end: String,
        title: String, place: String?, status: BlockStatus, source: BlockSource,
        locked: Boolean = false, tags: List<String> = emptyList(), memo: String? = null, todo: String = "",
    ) = Block(
        id = id, tripId = TRIP_ID, date = date, start = t(start), end = t(end),
        kind = FIXED, status = status, source = source,
        title = title, place = place, locked = locked, tags = tags, memo = memo, todo = todo,
    )
}
