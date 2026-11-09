package com.example.mbtrip.timeline.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * 여행 1건 저장 — 데이터 구조 문서의 Trip.
 *   경로  trips/{tripId}          (tripId = 문서 ID)
 *   destination  string  "시흥"
 *   startDate    string  "2026-10-24"
 *   endDate      string  "2026-10-25"   (startDate 이후)
 * + 앱 확장: updatedAt
 */
interface TripRepository {
    /** @param extra 문서 표에 없는 앱 확장 필드 (출발지·인원·숙소 시간 등) */
    suspend fun saveTrip(
        tripId: String, destination: String, startDate: LocalDate, endDate: LocalDate,
        extra: Map<String, Any?> = emptyMap(),
    )
}

/** 저장하지 않는 가짜 (미리보기·테스트용) */
object NoopTripRepository : TripRepository {
    override suspend fun saveTrip(
        tripId: String, destination: String, startDate: LocalDate, endDate: LocalDate, extra: Map<String, Any?>,
    ) = Unit
}

class FirestoreTripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : TripRepository {
    override suspend fun saveTrip(
        tripId: String, destination: String, startDate: LocalDate, endDate: LocalDate, extra: Map<String, Any?>,
    ) {
        require(!endDate.isBefore(startDate)) { "종료일은 시작일 이후여야 해요." }
        db.collection("trips").document(tripId).set(
            extra + mapOf(
                "destination" to destination,
                "startDate" to startDate.toString(),
                "endDate" to endDate.toString(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }
}
