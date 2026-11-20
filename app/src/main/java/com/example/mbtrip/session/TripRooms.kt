package com.example.mbtrip.session

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 여행 방 통째로 지우기 (내가 만든 방만).
 *
 *  Firestore 는 문서를 지워도 그 아래 하위 문서가 남아서, 하나씩 지운다:
 *   blocks(+ 각 블록의 votes · comments) · members · presence · reviews
 *  여행 문서는 { deleted: true } 표시만 남긴다 →
 *   - 함께하던 사람의 '내 여행' 목록에서 다음에 열 때 빠지고
 *   - 그 방 일정표를 보고 있던 사람에게는 "삭제된 방" 안내가 뜬다
 */
object TripRooms {
    private val db get() = FirebaseFirestore.getInstance()

    /** 반환: 문제가 있으면 안내 문구 */
    suspend fun deleteRoom(tripId: String): String? = runCatching {
        val trip = db.collection("trips").document(tripId)
        // 일정 블록 + 투표 · 댓글
        trip.collection("blocks").get().await().documents.forEach { b ->
            deleteAll(b.reference, "votes")
            deleteAll(b.reference, "comments")
        }
        listOf("blocks", "members", "presence", "reviews").forEach { deleteAll(trip, it) }
        // 지웠다는 표시 (초대코드도 지워서 더는 들어올 수 없게)
        trip.set(
            mapOf(
                "deleted" to true,
                "deletedAt" to FieldValue.serverTimestamp(),
                "inviteCode" to FieldValue.delete(),
            ),
            SetOptions.merge(),
        ).await()
        MyTrips.forget(tripId)
        if (TripSession.tripId == tripId) TripSession.clear()
        null
    }.getOrElse { e -> "방을 삭제하지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}" }

    /** 하위 문서 모두 지우기 (한 번에 400개씩) */
    private suspend fun deleteAll(parent: DocumentReference, sub: String) {
        while (true) {
            val docs = parent.collection(sub).limit(400).get().await().documents
            if (docs.isEmpty()) return
            val batch = db.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < 400) return
        }
    }
}
