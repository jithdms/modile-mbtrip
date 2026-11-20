package com.example.mbtrip.timeline.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/*
 * ───────────── 여러 사람이 같은 여행을 동시에 볼 때 ─────────────
 *  - 여행 정보(목적지·기간): trips/{tripId} 를 실시간으로 듣는다 → 한 사람이 기간을 넓히면 다른 폰의 날짜 탭도 바로 바뀐다
 *  - 지금 보고 있는 사람: trips/{tripId}/presence/{uid} 에 30초마다 '여기 있어요' 를 남긴다
 *  - 연결이 끊기거나 권한 오류가 나도 몇 초 뒤 다시 듣는다 (예전엔 한 번 끊기면 다시 안 붙었다)
 */

data class TripInfo(val destination: String?, val start: LocalDate?, val end: LocalDate?, val deleted: Boolean = false)

/** 함께 보는 사람 한 명 */
data class Viewer(val uid: String, val nickname: String)

/** 로그인(익명 포함)이 끝난 뒤에 듣기 시작 — 로그인 전에 들으면 권한 오류로 끊긴다 */
internal suspend fun ensureSignedIn(): String? {
    val auth = FirebaseAuth.getInstance()
    return auth.currentUser?.uid ?: runCatching { auth.signInAnonymously().await().user?.uid }.getOrNull()
}

/** 오류가 나면 잠깐 쉬었다가 다시 듣는다 (1초 → 2초 → … 최대 15초) */
internal fun <T> Flow<T>.keepListening(tag: String, fallback: T? = null): Flow<T> = retryWhen { cause, attempt ->
    android.util.Log.w("TripLive", "$tag 다시 연결 (${attempt + 1}번째)", cause)
    // 첫 실패 때는 빈 값을 먼저 보여 준다 (계속 '불러오는 중'에 머물지 않게)
    if (attempt == 0L && fallback != null) emit(fallback)
    delay(minOf(15_000L, 1_000L * (1L shl attempt.toInt().coerceAtMost(4))))
    true
}

object TripLive {
    private val db get() = FirebaseFirestore.getInstance()
    private fun trip(tripId: String) = db.collection("trips").document(tripId)

    fun observeTrip(tripId: String): Flow<TripInfo?> = flow {
        ensureSignedIn()
        emitAll(
            callbackFlow {
                val reg = trip(tripId).addSnapshotListener { snap, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snap == null || !snap.exists()) {
                        trySend(null)
                    } else {
                        trySend(
                            TripInfo(
                                destination = snap.getString("destination"),
                                start = snap.getString("startDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                                end = snap.getString("endDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                                deleted = snap.getBoolean("deleted") == true,
                            ),
                        )
                    }
                }
                awaitClose { reg.remove() }
            },
        )
    }.keepListening("trips/$tripId")

    /**
     * 여행 문서가 아직 없을 때만 만든다.
     * (예전엔 일정표를 열 때마다 내 폰의 날짜로 덮어써서, 친구가 바꾼 기간이 되돌아가곤 했다)
     */
    suspend fun ensureTrip(tripId: String, destination: String, start: LocalDate, end: LocalDate) {
        ensureSignedIn()
        val snap = trip(tripId).get().await()
        if (snap.exists() && snap.getString("startDate") != null) return
        TimelineDependencies.tripRepository.saveTrip(tripId, destination, start, end)
    }

    /* ── 지금 함께 보는 사람 ── */

    private const val HEARTBEAT_MS = 30_000L
    /** 이만큼 소식이 없으면 나간 걸로 본다 */
    private const val STALE_MS = 90_000L

    /** 일정표를 보는 동안 계속 '여기 있어요' 를 남긴다. 화면을 나가면(취소되면) 지운다 */
    suspend fun stayPresent(tripId: String, nickname: String) {
        val uid = ensureSignedIn() ?: return
        val ref = trip(tripId).collection("presence").document(uid)
        try {
            while (true) {
                runCatching {
                    ref.set(mapOf("uid" to uid, "nickname" to nickname, "at" to FieldValue.serverTimestamp())).await()
                }
                delay(HEARTBEAT_MS)
            }
        } finally {
            ref.delete() // 기다리지 않고 보낸다 (화면이 이미 닫히는 중)
        }
    }

    /** 지금 이 여행을 보고 있는 사람들 (나 포함) */
    fun observeViewers(tripId: String): Flow<List<Viewer>> = flow {
        ensureSignedIn()
        emitAll(
            callbackFlow {
                val reg = trip(tripId).collection("presence").addSnapshotListener { snap, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val now = System.currentTimeMillis()
                    trySend(
                        snap?.documents.orEmpty().mapNotNull { d ->
                            // 서버 시각이 아직 안 붙은(방금 쓴) 문서는 지금으로 친다
                            val at = d.getTimestamp("at")?.toDate()?.time ?: now
                            if (now - at > STALE_MS) null else Viewer(d.id, d.getString("nickname") ?: "친구")
                        },
                    )
                }
                awaitClose { reg.remove() }
            },
        )
    }.keepListening("presence/$tripId")
}
