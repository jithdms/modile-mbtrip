package com.example.mbtrip.notifications

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.mbtrip.R

/*
 * ★ 알림 동그라미 아이콘은 여기서 고른다 ★
 *
 * 종류마다 둘 중 하나를 쓰면 된다.
 *   1) 이모지:   emoji = "🗳️"
 *   2) 그림 파일: res/drawable 에 파일을 넣고 (예: ic_noti_vote.png 또는 벡터 xml)
 *                drawable = R.drawable.ic_noti_vote   ← drawable 이 있으면 이모지보다 먼저 쓴다
 *                (파일 이름은 영어 소문자·숫자·_ 만)
 *
 * background = 동그라미 바탕색 (기본 흰색), border = 테두리 색 (기본 검정, 아주 얇게)
 * tintIcon = 그림을 한 가지 색으로 칠하고 싶을 때 (null = 원래 색 그대로)
 */
data class NotificationIcon(
    val emoji: String = "🔔",
    @DrawableRes val drawable: Int? = null,
    val background: Color = Color.White,
    val border: Color = Color(0xFF1E1E1E),
    val tintIcon: Color? = null,
)

object NotificationIcons {
    // res/drawable 에 넣은 그림 (파일 이름 = R.drawable.이름)
    //  ⚠ 안드로이드는 파일 이름에 '-' 를 못 쓴다 → check-box.png 는 check_box.png 로 바꿔야 함
    val byType: Map<NotificationType, NotificationIcon> = mapOf(
        NotificationType.VOTE_REQUEST to NotificationIcon(emoji = "🗳️", drawable = R.drawable.vote),
        NotificationType.VOTE_RESULT to NotificationIcon(emoji = "✅", drawable = R.drawable.check_box),
        NotificationType.DELETE_PROPOSAL to NotificationIcon(emoji = "🗑️", drawable = R.drawable.litter),
        NotificationType.COMMENT to NotificationIcon(emoji = "💬", drawable = R.drawable.chat),
        NotificationType.SCHEDULE_CHANGE to NotificationIcon(emoji = "🗓️", drawable = R.drawable.calendar),
        NotificationType.INVITE to NotificationIcon(emoji = "👋", drawable = R.drawable.hello),
        NotificationType.APP_UPDATE to NotificationIcon(emoji = "📢", drawable = R.drawable.changes),
    )

    fun of(type: NotificationType): NotificationIcon = byType[type] ?: NotificationIcon()
}
