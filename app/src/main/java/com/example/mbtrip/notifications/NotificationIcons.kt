package com.example.mbtrip.notifications

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.mbtrip.R

/*
 * ★ 알림 동그라미 아이콘은 여기서 고른다 ★
 *
 * 종류마다 그림 파일을 고른다 (이모지는 쓰지 않는다).
 *   res/drawable 에 파일을 넣고 (예: ic_noti_vote.png 또는 벡터 xml)
 *   drawable = R.drawable.ic_noti_vote   (파일 이름은 영어 소문자·숫자·_ 만)
 *   drawable 이 없으면 기본 종 아이콘(ic_mb_bell)을 쓴다.
 *
 * background = 동그라미 바탕색 (기본 흰색), border = 테두리 색 (기본 연회색, 아주 얇게)
 * tintIcon = 그림을 한 가지 색으로 칠하고 싶을 때 (null = 원래 색 그대로)
 */
data class NotificationIcon(
    @DrawableRes val drawable: Int? = null,
    val background: Color = Color.White,
    val border: Color = Color(0xFFE1EDF2), // 얇은 회색 선
    val tintIcon: Color? = null,
)

object NotificationIcons {
    // res/drawable 에 넣은 그림 (파일 이름 = R.drawable.이름)
    //  ⚠ 안드로이드는 파일 이름에 '-' 를 못 쓴다 → check-box.png 는 check_box.png 로 바꿔야 함
    val byType: Map<NotificationType, NotificationIcon> = mapOf(
        NotificationType.VOTE_REQUEST to NotificationIcon(drawable = R.drawable.vote),
        NotificationType.VOTE_RESULT to NotificationIcon(drawable = R.drawable.check_box),
        NotificationType.DELETE_PROPOSAL to NotificationIcon(drawable = R.drawable.litter),
        NotificationType.COMMENT to NotificationIcon(drawable = R.drawable.chat),
        NotificationType.SCHEDULE_CHANGE to NotificationIcon(drawable = R.drawable.calendar),
        NotificationType.INVITE to NotificationIcon(drawable = R.drawable.hello),
        NotificationType.APP_UPDATE to NotificationIcon(drawable = R.drawable.changes),
        NotificationType.REVIEW_REQUEST to NotificationIcon(drawable = R.drawable.ic_mb_edit, background = Color(0xFFE6F8FC), tintIcon = Color(0xFF15809E)),
    )

    fun of(type: NotificationType): NotificationIcon = byType[type] ?: NotificationIcon()
}
