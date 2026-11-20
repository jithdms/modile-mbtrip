package com.example.mbtrip.triplog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mbtrip.notifications.AppNotification
import com.example.mbtrip.notifications.NotificationDependencies
import com.example.mbtrip.notifications.NotificationType
import com.example.mbtrip.session.MyTrip
import com.example.mbtrip.session.MyTrips
import com.example.mbtrip.session.TestAccess
import com.example.mbtrip.timeline.data.TripLive
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * ───────────── 여행이 끝나면 후기 알림 ─────────────
 *  - 오는 날이 지난 여행 중 내가 아직 후기를 안 쓴 여행마다 알림 하나 ("○○ 여행은 어떠셨나요?")
 *  - 알림을 누르면 후기 쓰기 화면 (ReviewWriteScreen) → 저장: trips/{tripId}/reviews/{내 uid}
 *  - 앱을 켤 때 · 알림 화면을 열 때 확인한다 (같은 여행은 알림 하나만)
 */
object ReviewReminders {
    private fun notificationId(tripId: String) = "review_$tripId"

    /** 후기 쓰기 화면에서 쓸 여행 (알림 → 화면으로 넘겨준다) */
    var target: MyTrip? = null

    /** 수정할 후기 (상세의 [수정] 아이콘). null = 새로 쓰기 */
    var editing: TripLog? = null

    /** 로그 → 후기 쓸 여행 (수정할 때) */
    fun tripOf(log: TripLog): MyTrip =
        tripFor(log.tripId) ?: MyTrip(log.tripId, log.destination, startDate = log.startDate, endDate = log.endDate)

    /** 이번에 후기를 쓴 여행 (일정표의 [후기 쓰러 가기] 를 바로 숨긴다) */
    var savedTrips: Set<String> by androidx.compose.runtime.mutableStateOf(emptySet()); private set

    /** 지난 여행 중 후기를 안 쓴 여행에 알림을 단다 */
    suspend fun check() {
        val today = LocalDate.now()
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val demo = if (TestAccess.isTester) TestAccess.demoTrips else emptyList()
        val ended = (MyTrips.trips + demo).distinctBy { it.tripId }.filter { t ->
            val end = t.endDate ?: t.startDate ?: return@filter false
            end.isBefore(today)
        }
        val repo = NotificationDependencies.repository
        ended.forEach { t ->
            val written = runCatching {
                FirebaseFirestore.getInstance().collection("trips").document(t.tripId)
                    .collection("reviews").document(me).get().await().exists()
            }.getOrDefault(false)
            if (written) {
                repo.remove(notificationId(t.tripId))
            } else {
                val end = t.endDate ?: t.startDate ?: today
                repo.post(
                    AppNotification(
                        id = notificationId(t.tripId),
                        type = NotificationType.REVIEW_REQUEST,
                        title = "${t.destination.ifBlank { "지난" }} 여행은 어떠셨나요?",
                        body = "여행이 끝났어요. 후기를 남기고 함께한 친구들과 추억을 나눠 보세요.",
                        // 여행이 끝난 다음 날 아침에 온 알림으로
                        createdAt = end.plusDays(1).atTime(9, 0).coerceAtMost(LocalDateTime.now()),
                        tripId = t.tripId,
                    ),
                )
            }
        }
    }

    /** 알림의 여행 ID → 후기 쓸 여행 */
    fun tripFor(tripId: String): MyTrip? =
        MyTrips.trips.firstOrNull { it.tripId == tripId } ?: TestAccess.demoTrips.firstOrNull { it.tripId == tripId }

    /** 후기 저장. 반환: 문제가 있으면 안내 문구 */
    suspend fun save(
        context: Context,
        trip: MyTrip,
        satisfaction: Int,
        title: String,
        content: String,
        /** 새로 고른 사진(Uri) 또는 이미 올린 사진 주소(String) */
        photos: List<Any>,
        /** 수정이면 원래 후기 (작성 시각은 그대로 둔다) */
        editing: TripLog? = null,
    ): String? {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return "로그인 정보를 가져오지 못했어요."
        if (satisfaction !in 1..5) return "만족도를 골라 주세요."
        if (content.isBlank()) return "후기 내용을 적어 주세요."
        return runCatching {
            val start = trip.startDate ?: LocalDate.now()
            // 여행 문서가 없으면(예: 체험용 여행) 만들어 둔다 — 로그 목록이 기간·장소를 여기서 읽는다
            TripLive.ensureTrip(trip.tripId, trip.destination, start, trip.endDate ?: start)
            val encoded = photos.take(MAX_PHOTOS).mapNotNull { p ->
                when (p) {
                    is Uri -> encode(context, p)
                    is String -> p // 이미 올린 사진은 그대로
                    else -> null
                }
            }
            val finalTitle = title.trim().ifBlank { "${trip.destination} 여행" }
            // 예시 로그(테스트 계정)는 DB 에 없어서 이 폰 화면에서만 바꾼다
            if (editing != null && editing.isSample) {
                applyEdit(editing.copy(satisfaction = satisfaction, title = finalTitle, content = content.trim(), photos = encoded))
                return null
            }
            val data = buildMap<String, Any> {
                put("id", me)
                put("tripId", trip.tripId)
                put("authorId", me)
                put("satisfaction", satisfaction)
                put("title", finalTitle)
                put("content", content.trim())
                put("photoUrls", encoded)
                if (editing == null) put("createdAt", FieldValue.serverTimestamp()) else put("updatedAt", FieldValue.serverTimestamp())
            }
            FirebaseFirestore.getInstance().collection("trips").document(trip.tripId)
                .collection("reviews").document(me)
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            // 수정이면 상세 화면 · 목록에 바로 반영
            if (editing != null) {
                applyEdit(editing.copy(satisfaction = satisfaction, title = finalTitle, content = content.trim(), photos = encoded))
            }
            NotificationDependencies.repository.remove(notificationId(trip.tripId))
            savedTrips = savedTrips + trip.tripId
            if (editing == null) TripLogStore.cachedLogs = null // 새 후기 → 로그 목록을 새로 읽게
            null
        }.getOrElse { e -> "후기를 저장하지 못했어요: ${e.message ?: "인터넷 연결을 확인해 주세요."}" }
    }

    /** 수정한 후기를 상세 · 목록 캐시에 바로 반영 */
    private fun applyEdit(updated: TripLog) {
        TripLogStore.selected = updated
        TripLogStore.cachedLogs = TripLogStore.cachedLogs?.map { if (it.key == updated.key) updated else it }
    }

    const val MAX_PHOTOS = 3

    /** 사진 → 긴 변 720px JPEG → "data:image/jpeg;base64,…" (문서 하나 1MB 제한 안에 3장) */
    private suspend fun encode(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 720) sample *= 2
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val scale = 720f / maxOf(bmp.width, bmp.height)
            val small = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
            val out = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 72, out)
            DATA_PHOTO_PREFIX + "jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }
}
