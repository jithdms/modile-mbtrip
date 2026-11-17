package com.example.mbtrip.timeline.model

/**
 * 여행 방 (Firestore: trips/{tripId}) — data/TripRepository.kt 가 쓰는 모델.
 * 일정표 블록은 이 문서 아래 trips/{tripId}/blocks/{blockId} 에 저장된다.
 *
 * Firestore 의 toObject(Trip::class.java) 로 읽으려면
 * 모든 값에 기본값이 있어야 한다 (빈 생성자가 필요하기 때문).
 */
data class Trip(
    val tripId: String = "",
    val title: String = "",
    val startDate: String = "",   // "2026-11-07"
    val endDate: String = "",     // "2026-11-08"
    val inviteCode: String = "",  // 6자리 초대 코드
    val createdBy: String = "",   // 만든 사람 uid
    val members: List<String> = emptyList(), // 참여자 uid 목록
)
