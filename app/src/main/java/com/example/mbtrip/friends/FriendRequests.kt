package com.example.mbtrip.friends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mbtrip.session.Account
import com.example.mbtrip.timeline.data.ensureSignedIn
import com.example.mbtrip.timeline.data.keepListening
import com.example.mbtrip.timeline.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * 친구 신청 → 상대가 알림에서 수락 → 서로 친구.
 *
 *  저장 위치: trips/_social/friendRequests/{보낸 uid}_{받는 uid}
 *    { from, to, fromProfile{…}, toProfile{…}, status, createdAt }
 *    status: pending(대기) → accepted(수락) / declined(거절) / unfriended(친구 삭제 알림)
 *  (trips 아래 문서 규칙이 '로그인한 사람' 이라 Firebase 규칙을 따로 바꾸지 않아도 된다)
 *
 *  흐름
 *   ① 보내기: 내가 문서를 pending 으로 만든다
 *   ② 받은 사람: 알림 화면 위 '친구 신청' 에서 [수락] → 내 친구 목록에 보낸 사람을 넣고 status = accepted
 *   ③ 보낸 사람: 앱이 친구 목록을 새로 읽을 때 accepted 를 보고 상대를 내 목록에 넣은 뒤 문서를 지운다
 *   친구 삭제: 내 목록에서 빼고 unfriended 문서를 남김 → 상대가 다음에 읽을 때 상대 목록에서도 빠진다
 */
data class FriendRequest(
    val id: String,
    val from: Member,
    val toUid: String,
)

object FriendRequests {
    private val db get() = FirebaseFirestore.getInstance()
    private fun col() = db.collection("trips").document("_social").collection("friendRequests")
    private fun myUid(): String? = FirebaseAuth.getInstance().currentUser?.uid
    private fun docId(from: String, to: String) = "${from}_$to"

    /** 내가 보내고 아직 답을 못 받은 사람들 (친구 추가 모달의 '신청함' 표시) */
    var sentPending: Set<String> by mutableStateOf(emptySet()); private set

    /** 나에게 온 대기 중인 신청 (실시간) */
    fun observeIncoming(): Flow<List<FriendRequest>> = flow {
        val me = ensureSignedIn() ?: run { emit(emptyList()); return@flow }
        emitAll(
            callbackFlow {
                val reg = col().whereEqualTo("to", me).whereEqualTo("status", "pending")
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snap?.documents.orEmpty().mapNotNull { it.toRequest() })
                    }
                awaitClose { reg.remove() }
            },
        )
    }.keepListening("friendRequests", fallback = emptyList())

    /** 친구 신청 보내기. 반환: 문제가 있으면 안내 문구 */
    suspend fun send(to: Member): String? {
        val me = myUid() ?: return "로그인한 뒤에 친구 신청을 보낼 수 있어요."
        if (to.userId == me) return "나 자신에게는 신청할 수 없어요."
        if (Friends.isFriend(to.userId)) return "이미 친구예요."
        return runCatching {
            // 상대가 이미 나에게 신청해 뒀으면 → 바로 수락
            val reverse = col().document(docId(to.userId, me)).get().await()
            if (reverse.exists() && reverse.getString("status") == "pending") {
                reverse.toRequest()?.let { r -> return accept(r) }
            }
            col().document(docId(me, to.userId)).set(
                mapOf(
                    "from" to me,
                    "to" to to.userId,
                    "fromProfile" to Friends.myselfMap(me),
                    "toProfile" to Friends.plainMap(to),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            sentPending = sentPending + to.userId
            null
        }.getOrElse { e ->
            android.util.Log.w("FriendRequests", "신청 실패", e)
            "친구 신청을 보내지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}"
        }
    }

    /** 수락: 내 목록에 넣고 보낸 사람에게 수락했다고 남긴다 */
    suspend fun accept(r: FriendRequest): String? {
        val me = myUid() ?: return "로그인 정보를 가져오지 못했어요."
        return runCatching {
            Friends.addToMyList(r.from)
            col().document(r.id).update(
                mapOf("status" to "accepted", "toProfile" to Friends.myselfMap(me), "answeredAt" to FieldValue.serverTimestamp()),
            ).await()
            null
        }.getOrElse { e -> "수락하지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}" }
    }

    /** 거절 */
    suspend fun decline(r: FriendRequest): String? = runCatching {
        col().document(r.id).update(mapOf("status" to "declined", "answeredAt" to FieldValue.serverTimestamp())).await()
        null
    }.getOrElse { e -> "거절하지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}" }

    /** 친구 삭제를 상대에게도 알린다 (상대 목록에서도 빠지게) */
    suspend fun notifyUnfriended(other: String) {
        val me = myUid() ?: return
        runCatching { col().document(docId(other, me)).delete().await() }
        runCatching {
            col().document(docId(me, other)).set(
                mapOf("from" to me, "to" to other, "status" to "unfriended", "createdAt" to FieldValue.serverTimestamp()),
            ).await()
        }
    }

    /**
     * 답을 받은 신청 정리 (친구 목록을 새로 읽을 때 부른다)
     *  - 내가 보낸 것: accepted → 상대를 내 목록에 / declined → 지움 / pending → '신청함' 표시
     *  - 나에게 온 것: unfriended → 상대를 내 목록에서 빼고 지움
     */
    suspend fun syncAnswers() {
        val me = myUid() ?: return
        runCatching {
            val sent = col().whereEqualTo("from", me).get().await().documents
            val pending = mutableSetOf<String>()
            sent.forEach { d ->
                val to = d.getString("to") ?: return@forEach
                when (d.getString("status")) {
                    "accepted" -> {
                        val profile = (d.get("toProfile") as? Map<*, *>)?.let { Friends.memberFrom(it, to) }
                        if (profile != null) Friends.addToMyList(profile)
                        d.reference.delete()
                    }
                    "declined" -> d.reference.delete()
                    "pending" -> pending += to
                }
            }
            sentPending = pending
        }
        runCatching {
            col().whereEqualTo("to", me).whereEqualTo("status", "unfriended").get().await().documents.forEach { d ->
                d.getString("from")?.let { Friends.removeFromMyList(it) }
                d.reference.delete()
            }
        }
    }

    fun clearLocal() {
        sentPending = emptySet()
    }

    private fun DocumentSnapshot.toRequest(): FriendRequest? {
        val from = getString("from") ?: return null
        val to = getString("to") ?: return null
        val profile = (get("fromProfile") as? Map<*, *>)?.let { Friends.memberFrom(it, from) }
            ?: Member(from, "친구", "@$from", jpScore = 50)
        return FriendRequest(id, profile, to)
    }
}
