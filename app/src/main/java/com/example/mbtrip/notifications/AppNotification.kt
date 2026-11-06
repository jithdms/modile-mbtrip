package com.example.mbtrip.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/** 알림 종류 — 필터가 이걸로 정해진다. 아이콘·색은 NotificationIcons.kt 에서 고른다 */
enum class NotificationType(val group: NotificationGroup) {
    VOTE_REQUEST(NotificationGroup.VOTE),        // "○○ 일정에 투표해 주세요"
    VOTE_RESULT(NotificationGroup.VOTE),         // "○○ 일정이 확정됐어요"
    DELETE_PROPOSAL(NotificationGroup.VOTE),     // "○○ 일정 삭제가 제안됐어요"
    COMMENT(NotificationGroup.SCHEDULE),         // 블록에 댓글
    SCHEDULE_CHANGE(NotificationGroup.SCHEDULE), // 시간·장소 변경
    INVITE(NotificationGroup.COMPANION),         // 여행에 초대됨 / 누가 들어옴
    APP_UPDATE(NotificationGroup.APP),           // 앱 업데이트·공지
}

/** 알림 화면 위쪽 필터 칸 */
enum class NotificationGroup(val label: String) {
    VOTE("투표"), SCHEDULE("일정"), COMPANION("동행"), APP("앱 소식"),
}

/**
 * 알림 1개.
 * @param tripId / blockId  눌렀을 때 어디로 갈지 (앱 소식은 둘 다 null)
 */
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime,
    val read: Boolean = false,
    val tripId: String? = null,
    val blockId: String? = null,
)

/**
 * 알림 저장소.
 * 지금은 메모리(가짜). 나중에 Firestore 로 옮길 땐 users/{uid}/notifications/{id} 에 두는 걸 추천
 * (알림은 '사람'에게 오는 거라 여행 아래가 아니라 사용자 아래가 맞다 — 데이터 구조 문서에 추가 필요).
 */
interface NotificationRepository {
    fun observe(): Flow<List<AppNotification>>
    fun unreadCount(): Flow<Int>
    fun markRead(id: String)
    fun markAllRead()
    fun remove(id: String)
    /** 다른 화면에서 알림 보내기 (예: 투표 요청, 삭제 제안) */
    fun post(notification: AppNotification)
}

class FakeNotificationRepository(initial: List<AppNotification> = SampleNotifications.make()) : NotificationRepository {
    private val items = MutableStateFlow(initial.sortedByDescending { it.createdAt })
    val state: StateFlow<List<AppNotification>> = items.asStateFlow()

    override fun observe(): Flow<List<AppNotification>> = items
    override fun unreadCount(): Flow<Int> = items.map { list -> list.count { !it.read } }
    override fun markRead(id: String) {
        items.value = items.value.map { if (it.id == id) it.copy(read = true) else it }
    }
    override fun markAllRead() {
        items.value = items.value.map { it.copy(read = true) }
    }
    override fun remove(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
    override fun post(notification: AppNotification) {
        items.value = (listOf(notification) + items.value.filterNot { it.id == notification.id })
            .sortedByDescending { it.createdAt }
    }
}

/** 화면 어디서나 같은 알림 목록을 쓰도록 한 곳에서 고른다 */
object NotificationDependencies {
    var repository: NotificationRepository = FakeNotificationRepository()
}

/** 가짜 알림 (거북섬 1박 시나리오, 지금 시각 기준으로 몇 분·몇 시간 전) */
object SampleNotifications {
    private const val TRIP = "trip_fake_geobukseom"

    fun make(now: LocalDateTime = LocalDateTime.now()): List<AppNotification> = listOf(
        AppNotification(
            "n1", NotificationType.VOTE_REQUEST, "투표해 주세요",
            "'갯벌 체험 (간조)' 일정 투표가 열렸어요. 의견을 남겨 주세요.",
            now.minusMinutes(5), tripId = TRIP, blockId = "b06",
        ),
        AppNotification(
            "n2", NotificationType.DELETE_PROPOSAL, "삭제 제안",
            "서연 님이 '일몰 보기' 삭제를 제안했어요 · \"비 소식이 있어서\"",
            now.minusMinutes(42), tripId = TRIP, blockId = "b08",
        ),
        AppNotification(
            "n3", NotificationType.COMMENT, "새 댓글",
            "민지: 웨이브파크 여기 주차 되나요?",
            now.minusHours(3), tripId = TRIP, blockId = "b03",
        ),
        AppNotification(
            "n4", NotificationType.VOTE_RESULT, "일정이 확정됐어요",
            "'웨이브파크 서핑' 일정이 동행자 3명 중 2명 찬성으로 확정됐어요.",
            now.minusHours(20), read = true, tripId = TRIP, blockId = "b03",
        ),
        AppNotification(
            "n5", NotificationType.INVITE, "동행자가 들어왔어요",
            "서연 님이 '거북섬 1박' 여행에 참여했어요.",
            now.minusDays(1).minusHours(2), read = true, tripId = TRIP,
        ),
        AppNotification(
            "n6", NotificationType.SCHEDULE_CHANGE, "일정이 바뀌었어요",
            "'저녁 후보' 시간이 17:30 → 18:00 으로 바뀌었어요.",
            now.minusDays(3), read = true, tripId = TRIP, blockId = "b09",
        ),
        AppNotification(
            "n7", NotificationType.APP_UPDATE, "MBTrip 1.1 업데이트",
            "일정 모달에서 가게 이름으로 장소를 찾고, 지도를 크게 볼 수 있어요.",
            now.minusDays(4),
        ),
    )
}
