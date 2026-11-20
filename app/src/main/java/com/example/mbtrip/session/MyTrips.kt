package com.example.mbtrip.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 내 여행 목록 한 줄 */
data class MyTrip(
    val tripId: String,
    val destination: String,
    val departure: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val headcount: Int? = null,
    val inviteCode: String? = null,
    /** 내가 만든 방이면 true, 초대코드로 들어간 방이면 false */
    val createdByMe: Boolean = true,
    /** 목록 정렬용 (마지막으로 만들거나 연 시각) */
    val touchedAt: Long = System.currentTimeMillis(),
)

/**
 * 내가 만들었거나 초대코드로 들어간 여행 방 목록.
 *
 * Firestore 구조상 여행(trips/{id})의 동행자는 하위 컬렉션(members)에 있어서,
 * "내가 들어간 여행 전체"를 한 번에 찾으려면 컬렉션 그룹 색인·권한 설정이 따로 필요하다.
 * 그래서 만들거나 들어간 여행을 두 곳에 적어 둔다.
 *  - Firestore users/{uid}/myTrips/{tripId}  → 다른 폰에서 로그인해도 같은 목록
 *  - 이 폰(SharedPreferences)                → 인터넷이 안 될 때도 바로 보이게
 * 목록을 열 때 서버 목록과 합치고, 각 여행 문서에서 날짜·목적지를 새로 읽어 맞춘다.
 *
 * MainActivity.onCreate 에서 MyTrips.init(context) 를 한 번 불러야 한다.
 */
object MyTrips {
    private const val PREFS = "mbtrip_my_trips"
    private const val KEY = "trips"
    private var prefs: SharedPreferences? = null

    /** 화면이 바로 바뀌도록 Compose 상태로 둔다 (최근 것이 위) */
    var trips: List<MyTrip> by mutableStateOf(emptyList()); private set

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        trips = load()
    }

    /** 새로 만들었거나 들어간 여행을 기억 (같은 ID 가 있으면 새 값으로 바꿔 맨 위로) */
    fun remember(trip: MyTrip) {
        trips = (listOf(trip) + trips.filterNot { it.tripId == trip.tripId }).sortedByDescending { it.touchedAt }
        persist()
        pushToServer(trip)
    }

    fun forget(tripId: String) {
        trips = trips.filterNot { it.tripId == tripId }
        persist()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mine(uid).document(tripId).delete()
    }

    /** 로그아웃 · 탈퇴 때: 이 폰의 목록만 비운다 (서버 목록은 그대로) */
    fun clearLocal() {
        lastPublished = null
        trips = emptyList()
        prefs?.edit()?.remove(KEY)?.apply()
    }

    /** 서버의 내 여행 목록을 받아 이 폰 목록과 합친다 (같은 여행은 최근 것) */
    suspend fun syncFromServer() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val remote = runCatching { mine(uid).get().await().documents.mapNotNull { it.toMyTrip() } }
            .getOrElse { e -> Log.w("MyTrips", "서버의 내 여행 목록을 읽지 못했어요", e); return }
        val merged = (remote + trips)
            .groupBy { it.tripId }
            .map { (_, same) -> same.maxBy { it.touchedAt } }
            .sortedByDescending { it.touchedAt }
        // 이 폰에만 있던 여행은 서버에도 올린다
        trips.filter { local -> remote.none { it.tripId == local.tripId } }.forEach { pushToServer(it) }
        if (merged != trips) {
            trips = merged
            persist()
        }
    }

    private fun mine(uid: String) =
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("myTrips")

    /** 서버에 한 줄 쓰기 (실패해도 이 폰 목록은 남아 있으니 조용히 넘어간다) */
    private fun pushToServer(t: MyTrip) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        mine(uid).document(t.tripId).set(
            buildMap {
                put("tripId", t.tripId)
                put("destination", t.destination)
                put("departure", t.departure)
                t.startDate?.let { put("startDate", it.toString()) }
                t.endDate?.let { put("endDate", it.toString()) }
                t.headcount?.let { put("headcount", it) }
                t.inviteCode?.let { put("inviteCode", it) }
                put("createdByMe", t.createdByMe)
                put("touchedAt", t.touchedAt)
            },
        ).addOnFailureListener { e -> Log.w("MyTrips", "내 여행 ${t.tripId} 을 서버에 올리지 못했어요", e) }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMyTrip(): MyTrip? {
        val id = getString("tripId") ?: this.id
        return MyTrip(
            tripId = id,
            destination = getString("destination") ?: "여행",
            departure = getString("departure").orEmpty(),
            startDate = getString("startDate")?.toDateOrNull(),
            endDate = getString("endDate")?.toDateOrNull(),
            headcount = getLong("headcount")?.toInt(),
            inviteCode = getString("inviteCode"),
            createdByMe = getBoolean("createdByMe") ?: true,
            touchedAt = getLong("touchedAt") ?: 0L,
        )
    }

    /** Firestore 에서 각 여행의 최신 목적지·날짜를 읽어 맞춘다. 실패한 여행은 그대로 둔다 */
    suspend fun refresh() {
        syncFromServer()
        val db = FirebaseFirestore.getInstance()
        val removed = mutableSetOf<String>()
        val updated = trips.map { t ->
            runCatching {
                val doc = db.collection("trips").document(t.tripId).get().await()
                if (!doc.exists()) return@runCatching t
                // 방장이 지운 방 → 내 목록에서도 뺀다
                if (doc.getBoolean("deleted") == true) {
                    removed += t.tripId
                    return@runCatching t
                }
                t.copy(
                    destination = doc.getString("destination") ?: t.destination,
                    departure = doc.getString("departure") ?: t.departure,
                    startDate = doc.getString("startDate")?.toDateOrNull() ?: t.startDate,
                    endDate = doc.getString("endDate")?.toDateOrNull() ?: t.endDate,
                    headcount = doc.getLong("headcount")?.toInt() ?: t.headcount,
                    inviteCode = doc.getString("inviteCode") ?: t.inviteCode,
                )
            }.getOrElse { e ->
                Log.w("MyTrips", "여행 ${t.tripId} 정보를 새로 읽지 못했어요", e)
                t
            }
        }
        if (updated != trips) {
            trips = updated
            persist()
        }
        removed.forEach { forget(it) }
    }

    /* ───── 저장 (JSON 한 줄) ───── */

    /**
     * 친구 화면의 ✈ '여행 중' 표시용 — 끝나지 않은 내 여행 기간을 내 프로필 문서(users/{uid}.travel)에 적어 둔다.
     * (내 여행 목록 자체는 나만 볼 수 있어서, 친구가 볼 수 있는 곳에 날짜만 따로)
     */
    private var lastPublished: String? = null
    private fun publishTravel() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = LocalDate.now()
        val list = trips
            .filter { (it.endDate ?: it.startDate)?.isBefore(today) == false && !it.tripId.startsWith("test_") }
            .take(20)
            .map { t ->
                mapOf(
                    "destination" to t.destination,
                    "start" to t.startDate.toString(),
                    "end" to (t.endDate ?: t.startDate).toString(),
                )
            }
        val key = list.toString()
        if (key == lastPublished) return
        lastPublished = key
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("travel" to list), com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { lastPublished = null }
    }

    private fun persist() {
        runCatching { publishTravel() }
        val arr = JSONArray()
        trips.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("tripId", t.tripId)
                    put("destination", t.destination)
                    put("departure", t.departure)
                    t.startDate?.let { put("startDate", it.toString()) }
                    t.endDate?.let { put("endDate", it.toString()) }
                    t.headcount?.let { put("headcount", it) }
                    t.inviteCode?.let { put("inviteCode", it) }
                    put("createdByMe", t.createdByMe)
                    put("touchedAt", t.touchedAt)
                },
            )
        }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }

    private fun load(): List<MyTrip> = runCatching {
        val arr = JSONArray(prefs?.getString(KEY, null) ?: return emptyList())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MyTrip(
                tripId = o.getString("tripId"),
                destination = o.optString("destination", "시흥"),
                departure = o.optString("departure", ""),
                startDate = o.optString("startDate").toDateOrNull(),
                endDate = o.optString("endDate").toDateOrNull(),
                headcount = if (o.has("headcount")) o.getInt("headcount") else null,
                inviteCode = o.optString("inviteCode").ifBlank { null },
                createdByMe = o.optBoolean("createdByMe", true),
                touchedAt = o.optLong("touchedAt", 0L),
            )
        }.sortedByDescending { it.touchedAt }
    }.getOrElse { emptyList() }
}

private fun String.toDateOrNull(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()
