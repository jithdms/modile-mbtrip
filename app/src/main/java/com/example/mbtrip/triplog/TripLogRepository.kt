package com.example.mbtrip.triplog

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mbtrip.session.TripSession
import com.example.mbtrip.timeline.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** 여행 모임 로그 저장소 */
interface TripLogRepository {
    /** 내가 참여한 여행들의 로그(후기) 전부 — 최근 여행 먼저 */
    suspend fun loadLogs(): List<TripLog>

    /** 그 여행을 함께한 사람들 */
    suspend fun loadMembers(tripId: String): List<Member>

    /** 예시 로그를 DB 에 넣는다 (처음 써 볼 때 화면이 비어 있지 않게) */
    suspend fun seedSamples()

    /** 내 로그(후기) 지우기 */
    suspend fun deleteLog(log: TripLog) = Unit
}

/** 이 로그를 지금 사용자가 지울 수 있는지 (내가 쓴 것만) */
fun TripLog.isMine(): Boolean {
    val me = FirebaseAuth.getInstance().currentUser?.uid
    return if (isSample) authorId == SampleTripLogs.ME else authorId == me
}

/** 화면이 쓰는 저장소를 한 곳에서 고른다 */
object TripLogDependencies {
    var repository: TripLogRepository = FirestoreTripLogRepository()

    /** DB 없이 화면만 볼 때 */
    val sample: TripLogRepository = SampleTripLogRepository
}

/**
 * 화면 사이에 넘겨줄 값 (목록 → 상세).
 * 라우트는 그대로 두고, 고른 로그만 여기에 적어 둔다.
 */
object TripLogStore {
    /** 지금 보는 로그 — 수정하면 상세 화면이 바로 바뀌도록 Compose 상태 */
    var selected: TripLog? by androidx.compose.runtime.mutableStateOf(null)
    var selectedPhotoIndex: Int = 0

    /** 마지막으로 불러온 목록 — 상세에서 돌아왔을 때 바로 보여주기 */
    var cachedLogs: List<TripLog>? = null
    var cachedIsSample: Boolean = false

    /** 상세에서 돌아왔을 때 보던 자리로 */
    var galleryScroll: Parcelable? = null

    /** 지운 로그 (예시 로그는 DB 에 없어서 화면에서만 숨긴다) */
    val deletedKeys: MutableSet<String> = mutableSetOf()

    /** 로그를 지운 뒤: 목록 캐시에서도 뺀다 */
    fun forget(log: TripLog) {
        deletedKeys += log.key
        cachedLogs = cachedLogs?.filterNot { it.key == log.key }
        if (selected?.key == log.key) selected = null
    }

    fun select(log: TripLog, photoIndex: Int) {
        selected = log
        selectedPhotoIndex = photoIndex
    }
}

/* ───────────────────────── Firestore ───────────────────────── */

class FirestoreTripLogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : TripLogRepository {

    private suspend fun myUid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
        ?: error("로그인 정보를 가져오지 못했어요.")

    override suspend fun loadLogs(): List<TripLog> = coroutineScope {
        val uid = myUid()
        myTripIds(uid)
            .map { id -> async { logsOfTrip(id) } }
            .awaitAll()
            .flatten()
            .sortedForGallery()
    }

    /**
     * 내 여행 목록 — 데이터 구조 문서: 컬렉션 그룹 members 에서 userId == 내 uid.
     * (Firebase 콘솔 → 색인 → 단일 필드 → members.userId 에 '컬렉션 그룹' 범위를 켜야 함)
     * 색인이 아직 없으면 이 폰이 아는 여행(지금 여행 + 예시 여행)만 직접 확인한다.
     */
    private suspend fun myTripIds(uid: String): Set<String> {
        val ids = linkedSetOf<String>()
        val viaGroup = runCatching {
            db.collectionGroup("members").whereEqualTo("userId", uid).get().await()
                .documents.mapNotNull { it.reference.parent.parent?.id }
        }.getOrNull()

        if (viaGroup != null) {
            ids += viaGroup
        } else {
            // 예시 여행은 테스트 계정만 확인한다
            val sampleIds = if (com.example.mbtrip.session.TestAccess.isTester) SampleTripLogs.tripIds else emptyList()
            (listOf(TripSession.currentTripId) + sampleIds).distinct().forEach { id ->
                val isMember = runCatching {
                    db.collection("trips").document(id).collection("members").document(uid).get().await().exists()
                }.getOrDefault(false)
                if (isMember) ids += id
            }
        }
        // 방금 만든 여행은 내가 멤버로 저장돼 있지 않아도 보여준다
        TripSession.tripId?.let { ids += it }
        return ids
    }

    private suspend fun logsOfTrip(tripId: String): List<TripLog> {
        val trip = db.collection("trips").document(tripId).get().await()
        if (!trip.exists()) return emptyList()
        val start = trip.getString("startDate").toLocalDateOrNull() ?: return emptyList()
        val end = trip.getString("endDate").toLocalDateOrNull()?.takeIf { !it.isBefore(start) } ?: start
        val destination = trip.getString("destination").orEmpty()
        return trip.reference.collection("reviews").get().await()
            .documents.map { it.toTripLog(tripId, destination, start, end) }
    }

    private fun DocumentSnapshot.toTripLog(tripId: String, destination: String, start: LocalDate, end: LocalDate) =
        TripLog(
            tripId = tripId,
            authorId = getString("authorId") ?: id,
            title = getString("title")?.takeIf { it.isNotBlank() } ?: "${destination.ifBlank { "우리" }} 여행",
            content = getString("content").orEmpty(),
            satisfaction = (getLong("satisfaction")?.toInt() ?: 0).coerceIn(0, 5),
            destination = destination,
            startDate = start,
            endDate = end,
            photos = (get("photoUrls") as? List<*>).orEmpty().filterIsInstance<String>().filter { it.isNotBlank() },
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        )

    override suspend fun deleteLog(log: TripLog) {
        if (log.isSample) return
        db.collection("trips").document(log.tripId).collection("reviews").document(log.authorId).delete().await()
    }

    override suspend fun loadMembers(tripId: String): List<Member> =
        db.collection("trips").document(tripId).collection("members").get().await()
            .documents
            .sortedBy { it.getTimestamp("joinedAt")?.seconds ?: 0L }
            .map { d ->
                Member(
                    userId = d.id,
                    nickname = d.getString("nickname") ?: d.getString("userId") ?: d.id,
                    handle = d.getString("handle") ?: "@${d.id}",
                    jpScore = d.getLong("jpScore")?.toInt() ?: 50,
                    photoUrl = d.getString("photoUrl"),
                    bio = d.getString("bio"),
                )
            }

    override suspend fun seedSamples() {
        val uid = myUid()
        val batch = db.batch()
        SampleTripLogs.trips.forEach { t ->
            val tripRef = db.collection("trips").document(t.tripId)
            batch.set(
                tripRef,
                mapOf(
                    "destination" to t.destination,
                    "startDate" to t.start.toString(),
                    "endDate" to t.end.toString(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            // 함께한 사람 — 나 + 예시 동행자
            batch.set(
                tripRef.collection("members").document(uid),
                mapOf(
                    "userId" to uid, "tripId" to t.tripId,
                    "nickname" to "나", "handle" to "@me",
                    "joinedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(), // 진단 점수(jpScore)는 지우지 않게
            )
            SampleTripLogs.companions.forEach { m ->
                batch.set(
                    tripRef.collection("members").document(m.userId),
                    mapOf(
                        "userId" to m.userId, "tripId" to t.tripId,
                        "nickname" to m.nickname, "handle" to m.handle,
                        "jpScore" to m.jpScore, "photoUrl" to m.photoUrl, "bio" to m.bio,
                        "joinedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }
            // 후기 — 문서 ID = 작성자 (한 사람당 1개)
            t.reviews.forEach { r ->
                val author = if (r.authorId == SampleTripLogs.ME) uid else r.authorId
                batch.set(
                    tripRef.collection("reviews").document(author),
                    mapOf(
                        "id" to author,
                        "tripId" to t.tripId,
                        "authorId" to author,
                        "satisfaction" to r.satisfaction,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "title" to r.title,
                        "content" to r.content,
                        "photoUrls" to r.photos,
                    ),
                )
            }
        }
        batch.commit().await()
    }
}

private fun String?.toLocalDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/* ───────────────────────── 예시 ───────────────────────── */

/** DB 없이 예시 로그만 보여주는 저장소 */
object SampleTripLogRepository : TripLogRepository {
    override suspend fun loadLogs(): List<TripLog> = SampleTripLogs.logs
    override suspend fun loadMembers(tripId: String): List<Member> = SampleTripLogs.membersWithMe
    override suspend fun seedSamples() = Unit
}

/** 예시 여행 3개 · 후기 4개 · 사진 11장 (예시 인물은 일정표의 가짜 동행자와 같음) */
object SampleTripLogs {
    const val ME = "me"

    internal data class SampleReview(
        val authorId: String, val title: String, val content: String, val satisfaction: Int, val photos: List<String>,
    )

    internal data class SampleTrip(
        val tripId: String, val destination: String, val start: LocalDate, val end: LocalDate, val reviews: List<SampleReview>,
    )

    val companions: List<Member> = listOf(
        Member("u_minji", "민지", "@minji_j", jpScore = 18, photoUrl = "res:user1", bio = "엑셀로 여행 계획 짜는 사람"),
        Member("u_junho", "준호", "@junho_p", jpScore = 72, photoUrl = "res:user2", bio = "발길 닿는 대로"),
        Member("u_seoyeon", "서연", "@seoyeon", jpScore = 45, photoUrl = "res:user3", bio = "바다랑 일몰 좋아해요"),
    )

    val membersWithMe: List<Member> = listOf(Member(ME, "나", "@me", jpScore = 50)) + companions

    internal val trips: List<SampleTrip> = listOf(
        SampleTrip(
            "log_sample_baegot", "시흥 배곧", LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 16),
            listOf(
                SampleReview(
                    "u_seoyeon", "배곧 한울공원 노을 산책",
                    "해 질 녘에 맞춰 바닷가 산책로를 걸었어요. 하늘이 분홍색에서 보라색으로 바뀌는 30분이 이번 여행의 하이라이트!\n저녁은 근처 조개구이로 마무리.",
                    5, listOf("res:photo2", "res:photo1"),
                ),
            ),
        ),
        SampleTrip(
            "log_sample_oido", "시흥 오이도", LocalDate.of(2026, 6, 6), LocalDate.of(2026, 6, 7),
            listOf(
                SampleReview(
                    "u_junho", "계획 없이 떠난 오이도",
                    "아침에 갑자기 출발했는데 생각보다 훨씬 좋았어요. 빨간 등대 앞에서 사진 잔뜩 찍고, 갯벌 체험은 다음에 꼭 다시 하고 싶어요.",
                    3, listOf("res:photo1", "res:photo2", "res:travel_main_photo", "res:photo1"),
                ),
            ),
        ),
        SampleTrip(
            "log_sample_geobukseom", "시흥 거북섬", LocalDate.of(2026, 4, 11), LocalDate.of(2026, 4, 13),
            listOf(
                SampleReview(
                    ME, "느리게 걸었던 거북섬 2박 3일",
                    "빡빡한 일정 대신 자연 속에서 걷고 쉬는 것에 집중했던 여행. 복잡한 일상에서 벗어나 재충전하기에 완벽한 2박 3일이었습니다.\n다음에는 서쪽 해안을 따라 렌터카 여행을 해보고 싶네요!",
                    4, listOf("res:travel_main_photo", "res:photo1", "res:photo2"),
                ),
                SampleReview(
                    "u_minji", "계획표보다 좋았던 웨이브파크",
                    "시간표를 10분 단위로 짰는데, 결국 제일 기억에 남는 건 계획에 없던 서핑 구경이었어요. 다음엔 직접 타 볼래요.",
                    5, listOf("res:photo2", "res:travel_main_photo"),
                ),
            ),
        ),
    )

    val tripIds: List<String> = trips.map { it.tripId }

    val logs: List<TripLog> = trips.flatMap { t ->
        t.reviews.mapIndexed { i, r ->
            TripLog(
                tripId = t.tripId, authorId = r.authorId, title = r.title, content = r.content,
                satisfaction = r.satisfaction, destination = t.destination,
                startDate = t.start, endDate = t.end, photos = r.photos,
                createdAtMillis = (t.reviews.size - i).toLong(), isSample = true,
            )
        }
    }.sortedForGallery()
}
