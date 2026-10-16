package com.example.mbtrip.timeline.data

import com.example.mbtrip.timeline.model.Block
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore 저장소 — Firebase 설정(google-services.json + 라이브러리)이 끝난 뒤에
 * app/src/main/java/com/example/mbtrip/timeline/data/ 에 넣는다.
 *
 * 저장 위치: trips/{tripId}/blocks/{blockId}
 * 실시간: addSnapshotListener — 다른 폰에서 바꾸면 곧바로 새 목록이 흘러온다.
 *         내가 쓴 내용은 서버 응답 전에 먼저 반영된다 (Firestore 로컬 캐시).
 */
class FirestoreBlockRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : BlockRepository {

    private fun blocksOf(tripId: String) =
        db.collection("trips").document(tripId).collection("blocks")

    override fun observeBlocks(tripId: String): Flow<List<Block>> = callbackFlow {
        val registration = blocksOf(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // ViewModel 이 "불러오지 못했어요" 알림을 띄움
                return@addSnapshotListener
            }
            val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.data?.let { blockFromFirestore(doc.id, tripId, it) } // 형식이 틀린 문서는 건너뜀
            }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun upsert(block: Block) {
        val data = block.toFirestoreMap() + (BlockFields.UPDATED_AT to FieldValue.serverTimestamp())
        blocksOf(block.tripId).document(block.id).set(data).await()
    }

    override suspend fun delete(tripId: String, blockId: String) {
        blocksOf(tripId).document(blockId).delete().await()
    }

    /** Firestore 가 만들어 주는 무작위 id — 두 사람이 동시에 추가해도 겹치지 않음 */
    override fun newId(tripId: String): String = blocksOf(tripId).document().id
}

/** Firebase Task 를 코루틴에서 기다리기 (play-services 코루틴 라이브러리 없이) */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
