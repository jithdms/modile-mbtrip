package com.example.mbtrip.siheung

import java.time.LocalTime

/*
 * ───────────── 추천 투어 루틴 ─────────────
 * 방 만들기 아래에서 미리보기로 보고 [이 순서로 돌아다니기] 를 누르면,
 * 여행을 만든 뒤 일정표에 이 순서대로 블록이 자동으로 들어간다 (PendingRoutine → TimelineScreen).
 * 장소는 모두 SiheungPlaces(팀 문서)의 이름 그대로.
 */

/** 루틴의 한 곳 */
data class RoutineStop(
    val placeName: String,
    val start: LocalTime,
    val end: LocalTime,
    /** 한 줄 팁 (블록 메모로 들어간다) */
    val tip: String? = null,
) {
    val place: SiheungPlace? get() = SiheungPlaces.named(placeName)
}

data class TourRoutine(
    val id: String,
    val title: String,
    val summary: String,
    /** 날마다 들르는 곳 (1일차, 2일차 …) */
    val days: List<List<RoutineStop>>,
    /** 어울리는 성향 한마디 */
    val mood: String,
) {
    val stopCount: Int get() = days.sumOf { it.size }
    val lengthLabel: String get() = if (days.size == 1) "당일" else "${days.size - 1}박 ${days.size}일"
}

private fun t(h: Int, m: Int = 0): LocalTime = LocalTime.of(h, m)
private fun stop(name: String, sh: Int, sm: Int, eh: Int, em: Int, tip: String? = null) =
    RoutineStop(name, t(sh, sm), t(eh, em), tip)

object TourRoutines {
    private val oido = listOf(
        stop("시흥오이도박물관", 10, 0, 11, 0, "오이도의 옛 모습부터 보고 시작해요"),
        stop("오이도빨강등대", 11, 15, 11, 45, "등대 앞 방파제가 사진 명소"),
        stop("생명의나무전망대", 11, 45, 12, 15),
        stop("청춘조개 오이도본점", 12, 30, 13, 40, "조개구이 점심"),
        stop("시흥 오이도 선사유적공원", 14, 0, 15, 0),
        stop("로아커피", 15, 15, 16, 20, "바다 보이는 창가 자리"),
        stop("배곧한울공원", 16, 50, 18, 0, "해 질 녘 산책으로 마무리"),
    )

    private val gaetgol = listOf(
        stop("갯골생태공원", 10, 0, 12, 0, "흔들전망대 · 소금창고 둘러보기"),
        stop("소래버섯나라", 12, 20, 13, 30, "버섯전골 점심"),
        stop("월곶포구", 13, 50, 14, 40),
        stop("베니어 베이커리카페", 15, 0, 16, 0),
        stop("미생의 다리", 16, 30, 17, 20, "노을 시간에 맞춰 가요"),
    )

    private val healing = listOf(
        stop("관곡지", 10, 0, 10, 50, "여름엔 연꽃이 한가득"),
        stop("연꽃테마파크", 10, 50, 11, 40),
        stop("봉순게장 물왕리점", 12, 0, 13, 10, "게장 정식 점심"),
        stop("물왕호수", 13, 20, 14, 20, "호수 한 바퀴 산책"),
        stop("빅트리", 14, 30, 15, 40, "큰 나무 카페에서 쉬어 가기"),
        stop("호조벌", 16, 10, 17, 0),
    )

    private val newtown = listOf(
        stop("배곧생명공원", 10, 0, 11, 0),
        stop("배곧한울공원", 11, 10, 12, 0, "바닷가 산책로"),
        stop("카츠오모이 배곧", 12, 10, 13, 10, "돈카츠 점심"),
        stop("캄", 13, 20, 14, 20),
        stop("해양생태과학관", 14, 40, 16, 0, "월요일은 쉬어요"),
        stop("노을조개 거북섬", 17, 30, 18, 40, "거북섬 노을 보며 저녁"),
    )

    private val sorae = listOf(
        stop("소래산", 9, 30, 11, 0, "정상까지 1시간 남짓"),
        stop("시흥 소래산 마애보살입상", 11, 0, 11, 30, "내려오는 길에 들러요"),
        stop("서운칼국수", 12, 0, 13, 0),
        stop("소전미술관", 13, 20, 14, 20),
        stop("청화공간", 14, 40, 15, 50, "한옥 느낌 카페"),
        stop("은계호수공원", 16, 10, 17, 0),
        stop("앞뜰 장작구이", 17, 30, 18, 40, "장작구이 저녁"),
    )

    val all: List<TourRoutine> = listOf(
        TourRoutine("oido", "오이도 바다 한 바퀴", "박물관 · 빨강등대 · 조개구이 · 노을", listOf(oido), "바다 좋아하는 여행"),
        TourRoutine("gaetgol", "갯골 · 월곶 생태 산책", "갯벌 공원과 포구, 노을 다리까지", listOf(gaetgol), "걷는 걸 좋아하는 여행"),
        TourRoutine("healing", "물왕호수 · 관곡지 힐링", "연꽃 · 호수 산책 · 숲속 카페", listOf(healing), "느긋한 P 여행"),
        TourRoutine("newtown", "배곧 · 거북섬 하루", "공원 · 과학관 · 거북섬 노을 저녁", listOf(newtown), "아이와 함께하는 여행"),
        TourRoutine("sorae", "소래산 등산 & 미술관", "가벼운 등산 뒤 미술관 · 카페", listOf(sorae), "아침형 J 여행"),
        TourRoutine("best2", "시흥 1박 2일 핵심", "첫날 바다, 둘째 날 호수와 연꽃", listOf(oido, healing), "처음 가는 시흥"),
    )

    fun byId(id: String): TourRoutine? = all.firstOrNull { it.id == id }
}

/**
 * [이 순서로 돌아다니기] 를 누른 루틴 — 그 여행의 일정표가 처음 열릴 때 블록으로 넣고 지운다.
 * 앱을 껐다 켜면 사라지는 메모리 값 (넣기 전에 앱을 끄면 다시 고르면 된다).
 */
object PendingRoutine {
    var routine: TourRoutine? = null; private set
    var tripId: String? = null; private set

    fun arm(routine: TourRoutine, tripId: String) {
        this.routine = routine
        this.tripId = tripId
    }

    /** 이 여행에 넣을 루틴이 있으면 꺼내고 비운다 */
    fun take(forTrip: String): TourRoutine? {
        if (tripId != forTrip) return null
        val r = routine
        routine = null
        tripId = null
        return r
    }
}
