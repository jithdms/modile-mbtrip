package com.example.mbtrip.diagnosis

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

    suspend fun saveJpScore(tripId: String, jpScore: Int): String {
        require(tripId.isNotBlank()) { "여행 방 정보가 필요해요." }
        require(jpScore in 0..100)
        // Firebase 대신 가짜 사용자 ID를 반환합니다.
        val dummyUserId = "local_user_id"
        saveJpScore(dummyUserId, tripId, jpScore)
        return dummyUserId
    }

    suspend fun saveJpScore(userId: String, tripId: String, jpScore: Int) {
        require(userId.isNotBlank()) { "로그인 사용자 정보가 필요해요." }
        require(tripId.isNotBlank()) { "여행 방 정보가 필요해요." }
        require(jpScore in 0..100)
        firestore.collection("trips")
            .document(tripId)
            .collection("members")
            .document(userId)
            .set(
                mapOf("userId" to userId, "tripId" to tripId, "jpScore" to jpScore),
                SetOptions.merge(),
            ).await()
    }

    /** Removes a member from one trip's participant list. */
    suspend fun removeMember(tripId: String, userId: String) {
        require(tripId.isNotBlank()) { "여행 방 정보가 필요해요." }
        require(userId.isNotBlank()) { "삭제할 사용자 정보가 필요해요." }
        firestore.collection("trips")
            .document(tripId)
            .collection("members")
            .document(userId)
            .delete()
            .await()
    }
}