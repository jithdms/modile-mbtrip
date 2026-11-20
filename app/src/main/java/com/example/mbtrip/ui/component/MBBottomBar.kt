package com.example.mbtrip.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mbtrip.R
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted

/** 하단 바 칸 */
enum class BottomTab(val label: String, @DrawableRes val icon: Int) {
    MyTrips("내 여행", R.drawable.ic_mb_luggage),
    TripLog("여행 로그", R.drawable.ic_mb_photos),
    Notifications("알림", R.drawable.ic_mb_bell),
    Friends("친구", R.drawable.ic_mb_group),
    MyPage("MY", R.drawable.ic_mb_person),
}

/**
 * 앱 하단 바 — 흰 바탕 + 위쪽 얇은 선, 아이콘만. 지금 화면의 칸(selected)은 진하게.
 * selected 가 null 이면(방 만들기 메인) 모든 칸이 같은 색.
 */
@Composable
fun MBBottomBar(
    selected: BottomTab?,
    onSelect: (BottomTab) -> Unit,
    hasUnreadNotifications: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        HorizontalDivider(color = Line, thickness = 1.dp)
        // 가로를 5칸으로 똑같이 나눈다 (여백 없이 → 누르면 칸 전체가 사각형으로 눌린다)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                BarItem(
                    tab = tab,
                    selected = tab == selected,
                    badge = tab == BottomTab.Notifications && hasUnreadNotifications,
                    modifier = Modifier.weight(1f),
                ) { onSelect(tab) }
            }
        }
    }
}

/** 한 칸 = 아이콘만 (글자 없음). 지금 화면 칸은 진하게 + 아래 작은 막대 */
@Composable
private fun BarItem(tab: BottomTab, selected: Boolean, badge: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) Ink else Muted, label = "barColor")
    val bar by animateColorAsState(if (selected) Ink else Color.Transparent, label = "barLine")
    Column(
        modifier
            // 모서리 없는 사각형 물결 (칸 전체), 크기 줄어드는 효과 없음
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            // 글자는 안 보이지만 화면 읽기(TalkBack)에는 이름을 알려 준다
            MBIcon(
                tab.icon, size = 26.dp, tint = color,
                contentDescription = if (badge) "${tab.label}, 안 읽은 알림 있음" else tab.label,
            )
            if (badge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .size(8.dp)
                        .background(Danger, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(width = 16.dp, height = 2.dp)
                .background(bar, RoundedCornerShape(50)),
        )
    }
}
