package com.example.mbtrip.travel

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/** A bearer invitation is single-use, expires after 12 hours and is replaced on reissue. */
class InviteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun uid() = auth.currentUser?.uid ?: error("로그인 후 이용해주세요.")

    suspend fun issue(active: ActiveTrip): String {
        val userId = uid()
        val code = InviteCode.generate()
        val expires = Timestamp(Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000L))
        val trip = db.collection("trips").document(active.id)
        db.runTransaction { tx ->
            val snapshot = tx.get(trip)
            require(snapshot.getString("ownerId") == userId) { "여행을 만든 사람만 초대할 수 있어요." }
            val old = snapshot.getString("inviteCode")
            tx.set(db.collection("tripInvites").document(code), mapOf(
                "tripId" to active.id, "ownerId" to userId, "expiresAt" to expires, "usedBy" to ""))
            tx.update(trip, "inviteCode", code)
            // Delete only tokens from this new invitation format, never legacy team documents.
            if (old != null && Regex("[0-9A-F]{32}").matches(old)) tx.delete(db.collection("tripInvites").document(old))
        }.await()
        return code
    }

    suspend fun revoke(active: ActiveTrip) {
        val userId = uid()
        val trip = db.collection("trips").document(active.id)
        db.runTransaction { tx ->
            val snapshot = tx.get(trip)
            require(snapshot.getString("ownerId") == userId) { "여행을 만든 사람만 취소할 수 있어요." }
            val old = snapshot.getString("inviteCode")
            tx.update(trip, "inviteCode", FieldValue.delete())
            if (old != null && Regex("[0-9A-F]{32}").matches(old)) tx.delete(db.collection("tripInvites").document(old))
        }.await()
    }

    suspend fun join(input: String): String {
        val code = InviteCode.normalize(input)
        val userId = uid()
        val invite = db.collection("tripInvites").document(code)
        return db.runTransaction { tx ->
            val snapshot = tx.get(invite)
            require(snapshot.exists()) { "초대 코드를 찾을 수 없어요. 새 코드를 요청해주세요." }
            val tripId = requireNotNull(snapshot.getString("tripId"))
            val member = db.collection("trips").document(tripId).collection("members").document(userId)
            // Own membership can be checked without access to the trip itself.
            if (tx.get(member).exists()) return@runTransaction tripId
            require(snapshot.getString("usedBy").isNullOrEmpty()) { "이미 사용한 코드예요. 새 코드를 요청해주세요." }
            require(snapshot.getTimestamp("expiresAt")?.toDate()?.after(Date()) == true) { "만료된 코드예요. 새 코드를 요청해주세요." }
            val profile = tx.get(db.collection("users").document(userId))
            val data = mutableMapOf<String, Any>("userId" to userId, "tripId" to tripId,
                "inviteCode" to code, "nickname" to ((profile.getString("nickname") ?: "여행자").take(20)),
                "handle" to "@${userId.takeLast(8)}")
            profile.getLong("jpScore")?.takeIf { it in 0..100 }?.let { data["jpScore"] = it }
            // Rules require both writes together. A code cannot be consumed or reused on its own.
            tx.update(invite, mapOf("usedBy" to userId, "usedAt" to FieldValue.serverTimestamp()))
            tx.set(member, data)
            tripId
        }.await()
    }
}
