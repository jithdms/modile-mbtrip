package com.example.mbtrip.friends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mbtrip.session.Account
import com.example.mbtrip.session.MyTrips
import com.example.mbtrip.session.TestAccess
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * 친구 목록.
 *  저장 위치 ① users/{내 uid} 문서의 friends 칸 { 친구 uid: { userId, nickname, handle, jpScore, photoUrl, bio } }
 *            → 내 문서라서 기본 보안 규칙만으로 항상 저장된다 (예전엔 ②만 써서 규칙을 안 바꾸면 추가가 안 됐다)
 *          ② users/{uid}/friends/{친구 uid} — 상대 목록에도 나를 넣을 때 (docs/firestore.rules 의 friends 규칙 필요)
 *  - 친구 추가는 서로 맺는다: 내 목록에 친구를, 친구 목록에 나를 함께 넣는다 (지울 때도 양쪽. 상대 쪽은 규칙이 있을 때만)
 *  - 목록을 열 때 users/{친구 uid} 를 다시 읽어서 바뀐 이름·소개를 반영한다
 *  - 테스트 계정은 가짜 친구 목록으로 시작하고, 추가·삭제는 이 폰 안에서만 된다
 *
 * 일정표의 '친구 초대' 모달은 이 목록에 있는 사람만 보여준다.
 */
object Friends {
    /** 지금 친구들 (화면이 바로 바뀌도록 Compose 상태) */
    var list: List<Member> by mutableStateOf(emptyList()); private set

    /** 지금 여행 중인 친구 → 여행지 (프로필 사진에 ✈ 표시) */
    var traveling: Map<String, String> by mutableStateOf(emptyMap()); private set

    fun travelingTo(userId: String): String? = traveling[userId]

    /** 한 번이라도 불러왔는지 */
    var loaded: Boolean by mutableStateOf(false); private set

    private val db get() = FirebaseFirestore.getInstance()
    private fun myUid(): String? = FirebaseAuth.getInstance().currentUser?.uid
    private fun friendsOf(uid: String) = db.collection("users").document(uid).collection("friends")

    /** 로그아웃할 때 */
    fun clearLocal() {
        FriendRequests.clearLocal()
        list = emptyList()
        traveling = emptyMap()
        loaded = false
        testerSeeded = false
    }

    private var testerSeeded = false

    /** 서버에서 다시 읽기 */
    suspend fun refresh() {
        if (TestAccess.isTester) {
            if (!testerSeeded) {
                list = FakeBlocks.members.sortedBy { it.nickname }
                // 테스트 계정: 준호는 여행 중인 것으로 보여 준다
                traveling = mapOf("u_junho" to "제주")
                testerSeeded = true
            }
            loaded = true
            return
        }
        val uid = myUid() ?: run { list = emptyList(); loaded = true; return }
        // 수락·친구 삭제 소식부터 반영 (내 문서의 friends 칸이 바뀐다)
        FriendRequests.syncAnswers()
        // ① 내 문서의 friends 칸 + ② 상대가 나를 추가해 준 friends 하위 문서
        val myDoc = db.collection("users").document(uid).get().await()
        val fromField = (myDoc.get("friends") as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
            (v as? Map<*, *>)?.toMember(k as? String ?: return@mapNotNull null)
        }
        val fromSub = runCatching { friendsOf(uid).get().await().documents.mapNotNull { it.toMember() } }.getOrDefault(emptyList())
        val saved = (fromField + fromSub).distinctBy { it.userId }.filter { it.userId != uid }
        val today = java.time.LocalDate.now()
        val nowTraveling = java.util.concurrent.ConcurrentHashMap<String, String>()
        list = coroutineScope {
            saved.map { savedMember ->
                async {
                    val saved = savedMember
                    // 최신 프로필로 (못 읽으면 저장해 둔 값) + 지금 여행 중인지
                    val doc = runCatching { db.collection("users").document(saved.userId).get().await() }.getOrNull()
                    (doc?.get("travel") as? List<*>).orEmpty().forEach { t ->
                        val m = t as? Map<*, *> ?: return@forEach
                        val s = (m["start"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return@forEach
                        val e = (m["end"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: s
                        if (!today.isBefore(s) && !today.isAfter(e)) nowTraveling[saved.userId] = (m["destination"] as? String).orEmpty()
                    }
                    doc?.toMember() ?: saved
                }
            }.awaitAll()
        }.sortedBy { it.nickname }
        traveling = nowTraveling.toMap()
        loaded = true
    }

    fun isFriend(userId: String): Boolean = list.any { it.userId == userId }

    /** 내 문서의 friends 칸에 넣기 (수락했을 때 · 상대가 수락했을 때) */
    internal suspend fun addToMyList(friend: Member) {
        val me = myUid() ?: return
        db.collection("users").document(me)
            .set(mapOf("friends" to mapOf(friend.userId to friend.toPlainMap())), SetOptions.merge())
            .await()
        list = (list.filterNot { it.userId == friend.userId } + friend).sortedBy { it.nickname }
    }

    /** 내 문서의 friends 칸에서 빼기 */
    internal suspend fun removeFromMyList(userId: String) {
        val me = myUid() ?: return
        runCatching { db.collection("users").document(me).update(FieldPath.of("friends", userId), FieldValue.delete()).await() }
        runCatching { friendsOf(me).document(userId).delete().await() }
        list = list.filterNot { it.userId == userId }
    }

    internal fun myselfMap(uid: String): Map<String, Any?> = myself(uid).toPlainMap()
    internal fun plainMap(m: Member): Map<String, Any?> = m.toPlainMap()
    internal fun memberFrom(map: Map<*, *>, uid: String): Member? = map.toMember(uid)

    /** 친구 추가. 반환: 문제가 있으면 안내 문구 */
    suspend fun add(friend: Member): String? {
        val me = myUid()
        if (friend.userId == me) return "나 자신은 친구로 추가할 수 없어요."
        if (isFriend(friend.userId)) return "이미 친구예요."
        if (TestAccess.isTester) {
            list = (list + friend).sortedBy { it.nickname }
            return null
        }
        me ?: return "로그인한 뒤에 친구를 추가할 수 있어요."
        return runCatching {
            // ① 내 문서에 (기본 규칙으로 항상 가능)
            db.collection("users").document(me)
                .set(mapOf("friends" to mapOf(friend.userId to friend.toPlainMap())), SetOptions.merge())
                .await()
            // ② 상대 목록에도 나를 (규칙을 아직 안 바꿨으면 조용히 건너뛴다)
            runCatching { friendsOf(friend.userId).document(me).set(myself(me).toMap()).await() }
            list = (list + friend).sortedBy { it.nickname }
            null
        }.getOrElse { e ->
            android.util.Log.w("Friends", "친구 추가 실패", e)
            "친구를 추가하지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}"
        }
    }

    /** 친구 삭제 (양쪽 목록에서). 반환: 문제가 있으면 안내 문구 */
    suspend fun remove(userId: String): String? {
        if (TestAccess.isTester) {
            list = list.filterNot { it.userId == userId }
            return null
        }
        val me = myUid() ?: return "로그인 정보를 가져오지 못했어요."
        return runCatching {
            db.collection("users").document(me).update(FieldPath.of("friends", userId), FieldValue.delete()).await()
            runCatching { friendsOf(me).document(userId).delete().await() }
            runCatching { friendsOf(userId).document(me).delete().await() }
            // 상대 목록에서도 빠지도록 알린다
            FriendRequests.notifyUnfriended(userId)
            list = list.filterNot { it.userId == userId }
            null
        }.getOrElse { "친구를 삭제하지 못했어요. 인터넷 연결을 확인해 주세요." }
    }

    /** 친구 추가 모달의 검색 — 가입한 사람 중 (나 빼고) */
    suspend fun searchPeople(query: String): List<Member> {
        val me = myUid()
        return TimelineDependencies.memberRepository.searchUsers(query).filter { it.userId != me }
    }

    /** 친구 추가 모달의 '함께 여행한 사람' — 같은 여행에 들어왔지만 아직 친구가 아닌 사람 */
    suspend fun travelMates(): List<Member> = coroutineScope {
        if (TestAccess.isTester) return@coroutineScope emptyList()
        val me = myUid()
        MyTrips.trips
            .filterNot { TestAccess.isDemoTrip(it.tripId) }
            .map { t ->
                async {
                    runCatching {
                        db.collection("trips").document(t.tripId).collection("members").get().await()
                            .documents.mapNotNull { it.toMember() }
                    }.getOrDefault(emptyList())
                }
            }
            .awaitAll()
            .flatten()
            .filter { it.userId != me && !isFriend(it.userId) }
            .distinctBy { it.userId }
            .sortedBy { it.nickname }
    }

    private fun myself(uid: String): Member {
        val p = Account.profile
        return Member(uid, p?.nickname ?: "친구", p?.handle ?: "@$uid", jpScore = p?.jpScore ?: 50, bio = p?.bio?.ifBlank { null })
    }

    private fun Member.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "nickname" to nickname,
        "handle" to handle,
        "jpScore" to jpScore,
        "photoUrl" to photoUrl,
        "bio" to bio,
        "addedAt" to FieldValue.serverTimestamp(),
    )

    private fun Member.toPlainMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "nickname" to nickname,
        "handle" to handle,
        "jpScore" to jpScore,
        "photoUrl" to photoUrl,
        "bio" to bio,
    )

    private fun Map<*, *>.toMember(uid: String): Member? {
        val nickname = this["nickname"] as? String ?: return null
        return Member(
            userId = uid,
            nickname = nickname,
            handle = this["handle"] as? String ?: "@$uid",
            jpScore = (this["jpScore"] as? Number)?.toInt() ?: 50,
            photoUrl = this["photoUrl"] as? String,
            bio = this["bio"] as? String,
        )
    }

    private fun DocumentSnapshot.toMember(): Member? {
        if (!exists()) return null
        val uid = getString("userId") ?: id
        return Member(
            userId = uid,
            nickname = getString("nickname") ?: return null,
            handle = getString("handle") ?: "@$uid",
            jpScore = getLong("jpScore")?.toInt() ?: 50,
            photoUrl = getString("photoUrl"),
            bio = getString("bio"),
        )
    }
}
