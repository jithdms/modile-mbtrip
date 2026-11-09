package com.example.mbtrip.notifications

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.ui.BackButton
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Black = Color(0xFF1E1E1E)
private val Sub = Color(0xFF757575)
private val Line = Color(0xFFEDEDED)
private val UnreadBg = Color(0xFFF2F8FD)   // 안 읽은 알림 = 아주 연한 하늘
private val UnreadDot = Color(0xFF1E88E5)


/**
 * 알림 모아보기 — 투표 요청·결과, 삭제 제안, 댓글, 일정 변경, 동행자, 앱 소식.
 *
 *  ◀  알림 (3)             모두 읽음
 *  [전체] [투표] [일정] [동행] [앱 소식]
 *  오늘
 *  (🗳️) 투표해 주세요            5분 전 ●
 *       준호 님이 '…' 일정을 제안했어요.
 *  어제 …
 *
 * @param onOpen 알림을 눌렀을 때 (읽음 처리는 이 화면이 한다). 예: 일정표로 이동
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpen: (AppNotification) -> Unit = {},
    repository: NotificationRepository = NotificationDependencies.repository,
) {
    val all by repository.observe().collectAsState(initial = emptyList())
    var filter by rememberSaveable { mutableStateOf<NotificationGroup?>(null) }
    val shown = if (filter == null) all else all.filter { it.type.group == filter }
    val unread = all.count { !it.read }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // 위: 뒤로 · 제목 · 모두 읽음
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            // 안 읽은 개수는 제목 옆 괄호에
            Text(
                if (unread > 0) "알림 ($unread)" else "알림",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "모두 읽음",
                fontSize = 13.sp,
                color = if (unread > 0) Black else Color(0xFFBDBDBD),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = unread > 0) { repository.markAllRead() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        // 필터
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip("전체", filter == null, count = all.count { !it.read }) { filter = null }
            NotificationGroup.entries.forEach { g ->
                FilterChip(g.label, filter == g, count = all.count { !it.read && it.type.group == g }) { filter = g }
            }
        }
        HorizontalDivider(color = Line)

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("아직 알림이 없어요", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("투표 요청이나 일정 변경이 생기면 여기에 모여요.", fontSize = 12.sp, color = Sub)
                }
            }
        } else {
            NotificationList(shown, onClick = { n -> repository.markRead(n.id); onOpen(n) })
        }
    }
}

@Composable
private fun NotificationList(shown: List<AppNotification>, onClick: (AppNotification) -> Unit) {
        val today = LocalDate.now()
        val grouped = shown.groupBy { dayLabel(it.createdAt.toLocalDate(), today) }
        LazyColumn(Modifier.fillMaxSize()) {
            grouped.forEach { (label, list) ->
                item(key = "h_$label") {
                    Text(
                        label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(list, key = { it.id }) { n ->
                    NotificationRow(n) { onClick(n) }
                    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 72.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) Black else Color.White)
            .border(1.dp, if (selected) Black else Color(0xFFD6D6D6), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = if (selected) Color.White else Black)
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else UnreadDot)
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (n.read) Color.White else UnreadBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val icon = NotificationIcons.of(n.type)
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(icon.background)
                .border(0.8.dp, icon.border, CircleShape), // 아주 얇은 검정 테두리
            contentAlignment = Alignment.Center,
        ) {
            if (icon.drawable != null) {
                Image(
                    painter = painterResource(icon.drawable),
                    contentDescription = null,
                    colorFilter = icon.tintIcon?.let { ColorFilter.tint(it) },
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(icon.emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    n.title,
                    fontSize = 15.sp,
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold,
                    color = Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(timeAgo(n.createdAt), fontSize = 11.sp, color = Sub)
                if (!n.read) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(UnreadDot))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                n.body, fontSize = 13.sp, color = if (n.read) Sub else Color(0xFF424242),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun dayLabel(day: LocalDate, today: LocalDate): String = when {
    day == today -> "오늘"
    day == today.minusDays(1) -> "어제"
    !day.isBefore(today.minusDays(6)) -> "이번 주"
    else -> "이전"
}

private val MD = DateTimeFormatter.ofPattern("M월 d일")

/** "방금 전" · "5분 전" · "3시간 전" · "2일 전" · "11월 1일" */
internal fun timeAgo(t: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val d = Duration.between(t, now)
    return when {
        d.toMinutes() < 1 -> "방금 전"
        d.toMinutes() < 60 -> "${d.toMinutes()}분 전"
        d.toHours() < 24 -> "${d.toHours()}시간 전"
        d.toDays() < 7 -> "${d.toDays()}일 전"
        else -> t.format(MD)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720, name = "알림 모아보기")
@Composable
private fun NotificationsPreview() {
    NotificationsScreen(onBack = {}, repository = FakeNotificationRepository())
}
