package com.example.mbtrip.session

import com.example.mbtrip.timeline.fake.FakeBlocks
import java.time.LocalDate

/**
 * 테스트 자료(가짜 친구 · 예시 알림 · 예시 일정 · 예시 여행 로그 · 체험용 여행)를 볼 수 있는 계정.
 *
 * 테스트 계정으로 로그인하면 앱 처음부터 끝까지 시나리오를 테스트 자료로 볼 수 있고,
 * 다른 계정에는 테스트 자료가 하나도 보이지 않는다 (실제로 만든 것만).
 *
 * 테스트 계정 정하는 방법 (둘 중 하나)
 *  1) 아래 TESTER_EMAILS 에 이메일 추가
 *  2) Firebase Console → Firestore → users/{그 사람 uid} 문서에 필드 tester = true (boolean) 추가
 */
object TestAccess {
    /** 테스트 계정 이메일 (소문자로) — 팀에서 쓰는 테스트 계정으로 바꿔 주세요 */
    val TESTER_EMAILS: Set<String> = setOf(
        "test@mbtrip.com",
    )

    /** 지금 로그인한 계정이 테스트 계정인지 */
    val isTester: Boolean get() = Account.profile?.tester == true

    fun isTesterEmail(email: String?): Boolean = email?.trim()?.lowercase() in TESTER_EMAILS

    /** 테스트 계정의 '내 여행'에 보이는 체험용 여행 (일정표에서 [테스트용 가짜 일정 넣기]로 채울 수 있음) */
    const val DEMO_TRIP_PREFIX = "test_"
    val demoTrips: List<MyTrip>
        get() = listOf(
            MyTrip(
                tripId = "${DEMO_TRIP_PREFIX}demo_siheung",
                destination = "시흥 (체험용)",
                departure = "서울",
                startDate = FakeBlocks.TRIP_START,
                endDate = FakeBlocks.TRIP_END,
                headcount = FakeBlocks.members.size,
                inviteCode = "TeSt01",
                createdByMe = true,
                touchedAt = 0L,
            ),
        )

    fun isDemoTrip(tripId: String?): Boolean = tripId?.startsWith(DEMO_TRIP_PREFIX) == true

    /** 테스트 자료 날짜를 못 쓸 때(일반 계정) 기본 여행 기간 = 오늘 하루 */
    fun defaultStart(): LocalDate = if (isTester) FakeBlocks.TRIP_START else LocalDate.now()
    fun defaultEnd(): LocalDate = if (isTester) FakeBlocks.TRIP_END else LocalDate.now()
}
