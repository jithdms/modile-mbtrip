package com.example.mbtrip.diagnosis

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Member 문서 저장소. Firebase 익명 인증 UID와 현재 여행 ID를 저장한다. */
class MemberRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun saveJpScore(tripId: String, jpScore: Int): String {
        require(tripId.isNotBlank()) { "여행 방 정보가 필요해요." }
        require(jpScore in 0..100)
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Firebase 사용자 인증에 실패했어요.")
        saveJpScore(user.uid, tripId, jpScore)
        return user.uid
    }

    suspend fun saveJpScore(userId: String, tripId: String, jpScore: Int) {
        require(userId.isNotBlank()) { "로그인 사용자 정보가 필요해요." }
        require(tripId.isNotBlank()) { "여행 방 정보가 필요해요." }
        require(jpScore in 0..100)
        val memberId = "${tripId}_$userId"
        firestore.collection("Member").document(memberId).set(
            mapOf("userId" to userId, "tripId" to tripId, "jpScore" to jpScore)
        ).await()
    }
}
