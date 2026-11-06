package com.example.mbtrip.timeline.vote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 투표를 데이터 구조 문서대로 Firestore 에 저장한다.
 *
 *  Vote     trips/{tripId}/blocks/{blockId}/votes/{userId}     ← 문서 ID = 투표자 → 한 사람당 1표 (다시 누르면 덮어쓰기)
 *           { blockId, voterId, value: "agree"|"disagree", createdAt }
 *  Comment  trips/{tripId}/blocks/{blockId}/comments/{자동 ID}   ← 의견(반대 사유 등)을 적었을 때만
 *           { blockId, authorId, content, createdAt }
 *
 * 확정 판정(동행자 과반 찬성 → confirmed)은 최재성 님 로직 담당 — 여기서는 저장만 한다.
 * 사용자 ID 는 Firebase 로그인 uid (로그인 전이면 진단 화면처럼 익명 로그인).
 */
class FirestoreVoteActions(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : VoteActions {

    private suspend fun uid(): String =
        auth.currentUser?.uid
            ?: auth.signInAnonymously().await().user?.uid
            ?: error("로그인 정보를 가져오지 못했어요.")

    private fun blockDoc(tripId: String, blockId: String) =
        db.collection("trips").document(tripId).collection("blocks").document(blockId)

    override suspend fun submitVote(
        tripId: String, blockId: String, choice: VoteChoice, comment: String,
    ): Result<Unit> = runCatching {
        val me = uid()
        val block = blockDoc(tripId, blockId)
        val batch = db.batch()
        batch.set(
            block.collection("votes").document(me),
            mapOf(
                "blockId" to blockId,
                "voterId" to me,
                "value" to when (choice) {
                    VoteChoice.AGREE -> "agree"
                    VoteChoice.DISAGREE -> "disagree"
                },
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        if (comment.isNotBlank()) {
            batch.set(
                block.collection("comments").document(),
                mapOf(
                    "blockId" to blockId,
                    "authorId" to me,
                    "content" to comment.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit().await()
        Unit
    }

    /** 수정 제안 내용도 댓글로 남긴다 */
    override suspend fun proposeEdit(tripId: String, blockId: String, summary: String): Result<Unit> = runCatching {
        val me = uid()
        blockDoc(tripId, blockId).collection("comments").document().set(
            mapOf(
                "blockId" to blockId,
                "authorId" to me,
                "content" to "[수정 제안] $summary",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }

    /** 삭제 제안 사유는 댓글로 남긴다 (블록의 deleteRequested 표시는 화면이 이미 저장함) */
    override suspend fun proposeDelete(tripId: String, blockId: String, reason: String): Result<Unit> = runCatching {
        val me = uid()
        blockDoc(tripId, blockId).collection("comments").document().set(
            mapOf(
                "blockId" to blockId,
                "authorId" to me,
                "content" to "[삭제 제안] ${reason.trim()}",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }
}
