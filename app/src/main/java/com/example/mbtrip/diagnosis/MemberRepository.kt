package com.example.mbtrip.diagnosis

/** Firebase를 사용하지 않도록 수정된 가짜(Fake) Member 문서 저장소입니다. */
class MemberRepository {

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
        // 파이어베이스 통신 없이 아무 동작도 하지 않거나, 로컬에 저장하는 로직으로 대체할 수 있습니다.
    }
}