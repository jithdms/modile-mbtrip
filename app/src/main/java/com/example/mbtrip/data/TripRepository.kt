package com.example.mbtrip.data

import com.example.mbtrip.timeline.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// 1. 6자리 랜덤 초대 코드 생성기
fun generateInviteCode(length: Int = 6): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..length).map { allowedChars.random() }.joinToString("")
}

// 2. 방 만들기 (Firestore에 Trip 저장) - 익명 로그인 자동 지원
fun createTrip(
    title: String,
    startDate: String,
    endDate: String,
    onSuccess: (String) -> Unit, // 성공 시 tripId 반환
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        // 로그인된 유저가 없다면 익명 로그인 후 방 생성 진행
        auth.signInAnonymously()
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    executeCreateTrip(db, userId, title, startDate, endDate, onSuccess, onFailure)
                } else {
                    onFailure(Exception("익명 로그인 유저 ID를 가져오지 못했습니다."))
                }
            }
            .addOnFailureListener { e ->
                onFailure(Exception("로그인(인증) 실패: ${e.message}"))
            }
    } else {
        executeCreateTrip(db, currentUser.uid, title, startDate, endDate, onSuccess, onFailure)
    }
}

// 내부 방 생성 실행 함수
private fun executeCreateTrip(
    db: FirebaseFirestore,
    userId: String,
    title: String,
    startDate: String,
    endDate: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val newTripRef = db.collection("trips").document()
    val tripId = newTripRef.id
    val inviteCode = generateInviteCode()

    val newTrip = Trip(
        tripId = tripId,
        title = title,
        startDate = startDate,
        endDate = endDate,
        inviteCode = inviteCode,
        createdBy = userId,
        members = listOf(userId)
    )

    newTripRef.set(newTrip)
        .addOnSuccessListener { onSuccess(tripId) }
        .addOnFailureListener { e -> onFailure(e) }
}

// 3. 초대 코드로 방 참여하기 - 익명 로그인 자동 지원
fun joinTripByInviteCode(
    inviteCode: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        auth.signInAnonymously()
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    executeJoinTrip(db, userId, inviteCode, onSuccess, onFailure)
                } else {
                    onFailure(Exception("익명 로그인 유저 ID를 가져오지 못했습니다."))
                }
            }
            .addOnFailureListener { e ->
                onFailure(Exception("로그인(인증) 실패: ${e.message}"))
            }
    } else {
        executeJoinTrip(db, currentUser.uid, inviteCode, onSuccess, onFailure)
    }
}

// 내부 방 참여 실행 함수
private fun executeJoinTrip(
    db: FirebaseFirestore,
    userId: String,
    inviteCode: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    db.collection("trips")
        .whereEqualTo("inviteCode", inviteCode)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                onFailure(Exception("유효하지 않은 초대 코드입니다."))
                return@addOnSuccessListener
            }

            val tripDoc = documents.documents[0]
            val tripId = tripDoc.id

            db.collection("trips").document(tripId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener { onSuccess(tripId) }
                .addOnFailureListener { e -> onFailure(e) }
        }
        .addOnFailureListener { e -> onFailure(e) }
}
fun getMyTrips(
    onSuccess: (List<Trip>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        onFailure(Exception("로그인된 사용자가 없습니다."))
        return
    }

    db.collection("trips")
        .whereArrayContains("members", currentUser.uid)
        .get()
        .addOnSuccessListener { documents ->
            val tripList = documents.mapNotNull { doc ->
                doc.toObject(Trip::class.java)
            }
            onSuccess(tripList)
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}