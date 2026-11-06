package com.example.mbtrip.session

import java.security.SecureRandom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mbtrip.createtrip.TripSetup
import com.example.mbtrip.createtrip.dateRange
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 지금 만들고 있는(또는 보고 있는) 여행 1건 — 화면 순서대로 값을 모은다.
 *
 *  ① 방 만들기 [생성하기]   → start(): 출발·도착(목적지)·기간·명수 + 새 tripId
 *                            saveBasics(): Firestore 에 trips/{tripId} + members/{내 uid} 저장
 *  ② 친구 초대              → members/{친구 uid} (초대 모달이 저장)
 *  ③ 숙소·교통 [저장하기]   → save(): 숙소·교통 값과 넓어진 기간을 trips/{tripId} 에 더한다
 *  ④ 일정표                → tripId 로 blocks 를 읽고 쓴다
 *
 * 아직 여행을 안 만들었으면(개발용 화면 목록에서 바로 열었을 때) 가짜 여행(trip_fake_geobukseom)을 쓴다.
 */
object TripSession {
    var tripId: String? = null; private set
    var departure: String = ""; private set
    var destination: String = "시흥"; private set
    var startDate: LocalDate? = null; private set
    var endDate: LocalDate? = null; private set
    var headcount: Int? = null; private set

    /** 초대코드 (영어 대소문자 + 숫자 6자리, 예: aB3x9Q). 화면이 바로 바뀌도록 Compose 상태로 둔다 */
    var inviteCode: String? by mutableStateOf(null); private set

    /** 화면에서 쓸 여행 ID (만든 여행이 없으면 가짜 여행) */
    val currentTripId: String get() = tripId ?: FakeBlocks.TRIP_ID

    /**
     * ① 방 만들기 [생성하기]. 날짜 글자는 "2026.10.01 ~ 2026.10.03" 같은 모양을 읽는다.
     * 반환: 문제가 있으면 안내 문구, 없으면 null
     */
    fun start(departure: String, arrival: String, dateRangeText: String, headcountText: String): String? {
        val range = parseDateRange(dateRangeText) ?: return "날짜를 2026.10.01 ~ 2026.10.03 처럼 적어 주세요."
        if (range.second.isBefore(range.first)) return "끝나는 날이 시작하는 날보다 빨라요."
        tripId = FirebaseFirestore.getInstance().collection("trips").document().id // 겹치지 않는 새 ID
        this.departure = departure.trim()
        this.destination = arrival.trim().ifBlank { "시흥" }
        startDate = range.first
        endDate = range.second
        headcount = Regex("\\d+").find(headcountText)?.value?.toIntOrNull()
        inviteCode = newInviteCode()
        return null
    }

    /** ① 직후: 여행 문서 + 나(진단 점수 포함)를 첫 동행자로 저장 → 친구 초대 화면에서 바로 보인다 */
    suspend fun saveBasics(myJpScore: Int?) {
        val id = tripId ?: return
        val start = startDate ?: LocalDate.now()
        // 초대코드가 다른 여행과 겹치면 새로 뽑는다 (62^6 ≈ 568억 가지라 거의 안 겹침)
        var code = inviteCode ?: newInviteCode()
        for (attempt in 1..5) {
            val owner = findTripIdByCode(code)
            if (owner == null || owner == id) break
            code = newInviteCode()
        }
        inviteCode = code
        TimelineDependencies.tripRepository.saveTrip(
            tripId = id,
            destination = destination,
            startDate = start,
            endDate = endDate ?: start,
            extra = buildMap {
                put("departure", departure)
                headcount?.let { put("headcount", it) }
                put("inviteCode", code) // 앱 확장: 초대코드
            },
        )
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: return
        TimelineDependencies.memberRepository.invite(id, listOf(Member(uid, "나", "@me", jpScore = myJpScore ?: 50)))
    }

    /**
     * ③ 숙소·교통 [저장하기] 뒤: 숙소·교통 값을 여행 문서에 더한다.
     * 숙소·교통에서 고른 날짜가 더 넓으면 기간을 그만큼 넓힌다.
     */
    suspend fun save(setup: TripSetup) {
        val id = tripId ?: return // 방 만들기를 거치지 않았으면 저장하지 않음 (가짜 여행)
        setup.dateRange()?.let { (s, e) ->
            startDate = listOfNotNull(startDate, s).min()
            endDate = listOfNotNull(endDate, e).max()
        }
        val start = startDate ?: LocalDate.now()
        val end = endDate ?: start
        TimelineDependencies.tripRepository.saveTrip(
            tripId = id,
            destination = destination,
            startDate = start,
            endDate = end,
            extra = buildMap {
                put("departure", departure)                 // 앱 확장: 출발지
                headcount?.let { put("headcount", it) }      // 앱 확장: 예정 인원
                put("dayTrip", setup.dayTrip)                // 앱 확장: 당일치기
                setup.checkIn?.let { put("checkIn", it.toString()) }
                setup.checkOut?.let { put("checkOut", it.toString()) }
            },
        )
    }

    /**
     * 초대코드로 다른 사람의 여행에 들어가기.
     * 반환: 문제가 있으면 안내 문구, 성공하면 null (그 여행이 '지금 여행'이 된다)
     */
    suspend fun joinByCode(code: String, myJpScore: Int?): String? {
        val c = code.trim()
        if (!INVITE_CODE_REGEX.matches(c)) return "초대코드는 영어 대소문자·숫자 6자리예요."
        val doc = FirebaseFirestore.getInstance().collection("trips")
            .whereEqualTo("inviteCode", c).limit(1).get().await()
            .documents.firstOrNull() ?: return "'$c' 초대코드를 가진 여행이 없어요. 대소문자까지 확인해 주세요."

        tripId = doc.id
        destination = doc.getString("destination") ?: "시흥"
        departure = doc.getString("departure").orEmpty()
        startDate = doc.getString("startDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        endDate = doc.getString("endDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        headcount = doc.getLong("headcount")?.toInt()
        inviteCode = c

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
            ?: return "로그인 정보를 가져오지 못했어요."
        TimelineDependencies.memberRepository.invite(doc.id, listOf(Member(uid, "나", "@me", jpScore = myJpScore ?: 50)))
        return null
    }

    private suspend fun findTripIdByCode(code: String): String? = runCatching {
        FirebaseFirestore.getInstance().collection("trips")
            .whereEqualTo("inviteCode", code).limit(1).get().await()
            .documents.firstOrNull()?.id
    }.getOrNull()

    /** 집 버튼 → 처음부터 다시 */
    fun clear() {
        tripId = null; departure = ""; destination = "시흥"
        startDate = null; endDate = null; headcount = null; inviteCode = null
    }
}

/** 초대코드: 영어 대소문자 + 숫자 6자리 */
internal val INVITE_CODE_REGEX = Regex("^[A-Za-z0-9]{6}$")
private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
private val random = SecureRandom()

internal fun newInviteCode(): String = buildString { repeat(6) { append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]) } }

private val DOT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")

/** "2026.10.01 ~ 2026.10.03" / "2026-10-01~2026-10-03" / "2026.10.01" (하루) → (시작, 끝) */
internal fun parseDateRange(text: String): Pair<LocalDate, LocalDate>? {
    val found = Regex("(\\d{4})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})").findAll(text).mapNotNull { m ->
        val (y, mo, d) = m.destructured
        runCatching { LocalDate.parse("$y.$mo.$d", DOT_DATE) }.getOrNull()
    }.toList()
    return when (found.size) {
        0 -> null
        1 -> found[0] to found[0]
        else -> found[0] to found[1]
    }
}
