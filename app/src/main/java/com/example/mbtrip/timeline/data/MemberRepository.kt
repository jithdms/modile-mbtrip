package com.example.mbtrip.timeline.data

import com.google.firebase.firestore.SetOptions
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 동행자(여행 멤버) 저장소 — 상단 `+` → 친구 초대 모달이 쓴다.
 *  - FakeMemberRepository     : 메모리 (가짜 친구 목록으로 검색·초대 연습)
 *  - FirestoreMemberRepository: Firebase (회원가입으로 users 컬렉션이 채워진 뒤 TimelineDependencies 에서 바꿔 끼움)
 */
interface MemberRepository {
    /** 이 여행의 동행자 목록 (실시간) */
    fun observeMembers(tripId: String): Flow<List<Member>>

    /** 닉네임 또는 @아이디로 사람 찾기. 빈 글자면 추천(처음 몇 명) */
    suspend fun searchUsers(query: String): List<Member>

    /** 동행자로 추가 (이미 있으면 그대로) */
    suspend fun invite(tripId: String, members: List<Member>)

    /** 동행에서 빼기 (내보내기 · 나가기 공용) */
    suspend fun remove(tripId: String, userId: String)
}

/** '@' 떼고 소문자로 — 검색 비교용 */
internal fun normalizeQuery(q: String): String = q.trim().removePrefix("@").lowercase()

/* ───────────────────────── 가짜 ───────────────────────── */

class FakeMemberRepository(
    /** 검색으로 찾을 수 있는 '앱 사용자' 들 (예시 인물) */
    private val users: List<Member> = FakeUsers.all,
) : MemberRepository {
    private val byTrip = MutableStateFlow<Map<String, List<Member>>>(emptyMap())

    /** 처음 보는 여행이면 가짜 일정의 동행자 3명으로 시작 */
    private fun membersOf(map: Map<String, List<Member>>, tripId: String) = map[tripId] ?: FakeBlocks.members

    override fun observeMembers(tripId: String): Flow<List<Member>> = byTrip.map { membersOf(it, tripId) }

    override suspend fun searchUsers(query: String): List<Member> {
        val q = normalizeQuery(query)
        if (q.isEmpty()) return users.take(10)
        return users.filter {
            it.nickname.lowercase().contains(q) || it.handle.removePrefix("@").lowercase().contains(q)
        }
    }

    override suspend fun invite(tripId: String, members: List<Member>) {
        val now = byTrip.value
        val current = membersOf(now, tripId)
        val added = members.filter { m -> current.none { it.userId == m.userId } }
        byTrip.value = now + (tripId to current + added)
    }

    override suspend fun remove(tripId: String, userId: String) {
        val now = byTrip.value
        byTrip.value = now + (tripId to membersOf(now, tripId).filterNot { it.userId == userId })
    }
}

/** 초대 연습용 가짜 사용자 (예시 인물) */
object FakeUsers {
    val all: List<Member> = FakeBlocks.members + listOf(
        Member("u_haneul", "하늘", "@haneul_trip", jpScore = 30, bio = "카페 투어 전문"),
        Member("u_doyun", "도윤", "@doyun", jpScore = 65, bio = "서핑 배우는 중 🏄"),
        Member("u_yerin", "예린", "@yerin_j", jpScore = 10, bio = "10분 단위로 일정 짜요"),
        Member("u_jiho", "지호", "@jiho_p", jpScore = 85),
        Member("u_suah", "수아", "@sua_a", jpScore = 40, bio = "사진 찍어주는 담당 📸"),
        Member("u_minjun", "민준", "@minjun22", jpScore = 52),
        Member("u_chaeeun", "채은", "@chaeeun", jpScore = 23),
    )
}

/* ───────────────────────── Firestore ───────────────────────── */

/**
 * 저장 위치
 *  - 사용자:   users/{userId}             { nickname, handle("@아이디"), handleLower, nicknameLower, jpScore, photoUrl, bio(한 줄 소개) }
 *  - 동행자:   trips/{tripId}/members/{userId}  ← 데이터 구조 문서의 Member
 *              { userId, tripId, jpScore(0=J~100=P) }  + 앱 확장 { nickname, handle, photoUrl, bio, joinedAt }
 *
 * 검색은 Firestore 특성상 '앞글자 일치'만 된다 (예: "min" → minji, minjun).
 * 그래서 회원가입 때 handleLower(@ 뗀 소문자), nicknameLower 를 같이 저장해야 한다.
 */
class FirestoreMemberRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MemberRepository {

    private fun membersOf(tripId: String) = db.collection("trips").document(tripId).collection("members")

    override fun observeMembers(tripId: String): Flow<List<Member>> = callbackFlow {
        // joinedAt 이 없는 문서(진단 화면이 먼저 만든 Member)도 보이게 정렬은 앱에서 한다
        val reg = membersOf(tripId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(
                snap?.documents.orEmpty()
                    .sortedBy { it.getTimestamp("joinedAt")?.seconds ?: 0L }
                    .mapNotNull { it.toMember() },
            )
        }
        awaitClose { reg.remove() }
    }

    override suspend fun searchUsers(query: String): List<Member> {
        val found = runCatching { searchFirestoreUsers(query) }.getOrDefault(emptyList())
        // 회원가입이 아직 없어 users 가 비어 있으면 가짜 친구 목록에서 찾는다 (초대 연습용)
        return found.ifEmpty { FakeMemberRepository().searchUsers(query) }
    }

    private suspend fun searchFirestoreUsers(query: String): List<Member> {
        val users = db.collection("users")
        val q = normalizeQuery(query)
        if (q.isEmpty()) return users.limit(10).get().await().documents.mapNotNull { it.toMember() }
        val byHandle = users.orderBy("handleLower").startAt(q).endAt(q + "").limit(20).get().await()
        val byName = users.orderBy("nicknameLower").startAt(q).endAt(q + "").limit(20).get().await()
        return (byHandle.documents + byName.documents).mapNotNull { it.toMember() }.distinctBy { it.userId }
    }

    override suspend fun invite(tripId: String, members: List<Member>) {
        val batch = db.batch()
        members.forEach { m ->
            batch.set(
                membersOf(tripId).document(m.userId),
                mapOf(
                    // ── 데이터 구조 문서의 Member 필드
                    "userId" to m.userId,
                    "tripId" to tripId,
                    // ── 앱 확장 필드
                    "nickname" to m.nickname,
                    "handle" to m.handle,
                    "jpScore" to m.jpScore,
                    "photoUrl" to m.photoUrl,
                    "bio" to m.bio,
                    "joinedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(), // 진단에서 저장한 jpScore 등 다른 필드는 지우지 않게
            )
        }
        batch.commit().await()
    }

    override suspend fun remove(tripId: String, userId: String) {
        membersOf(tripId).document(userId).delete().await()
    }

    private fun DocumentSnapshot.toMember(): Member? {
        // 닉네임이 아직 없는 Member(진단 점수만 저장된 경우)는 아이디로 대신 보여준다
        val nickname = getString("nickname") ?: getString("userId") ?: id
        return Member(
            userId = id,
            nickname = nickname,
            handle = getString("handle") ?: "@$id",
            jpScore = getLong("jpScore")?.toInt() ?: 50,
            photoUrl = getString("photoUrl"),
            bio = getString("bio"),
        )
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
