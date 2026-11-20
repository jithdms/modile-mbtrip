package com.example.mbtrip.notifications

import com.example.mbtrip.friends.FriendRequests
import kotlinx.coroutines.launch

import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.R
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
import androidx.compose.runtime.remember
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.mbtrip.ui.design.MBChip
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua600
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.MBBrush
import com.example.mbtrip.ui.theme.Muted
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Black = Ink
private val Sub = Muted
private val Line = com.example.mbtrip.ui.theme.Line
private val UnreadBg = Aqua100   // 안 읽은 알림 = 연한 브랜드 아쿠아
private val UnreadDot = Aqua600


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

    SkyBackground(Modifier.fillMaxSize(), intensity = 0.6f) {
    Column(Modifier.fillMaxSize()) {
        // 위: 뒤로 · 제목 · 모두 읽음
        ScreenHeader(
            // 안 읽은 개수는 제목 옆 괄호에
            title = if (unread > 0) "알림 ($unread)" else "알림",
            onBack = onBack,
        ) {
            Text(
                "모두 읽음",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (unread > 0) Aqua700 else Faint,
                modifier = Modifier
                    .bouncyClick(RoundedCornerShape(50), enabled = unread > 0, scaleDown = 0.9f) { repository.markAllRead() }
                    .background(if (unread > 0) Aqua100 else Color.Transparent, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }

        // 친구 신청 — 수락하면 서로 친구 목록에 올라간다
        FriendRequestSection()

        // 필터
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MBChip(
                "전체", filter == null, onClick = { filter = null },
                count = all.count { !it.read }, modifier = Modifier.enterPop(0),
            )
            NotificationGroup.entries.forEachIndexed { i, g ->
                MBChip(
                    g.label, filter == g, onClick = { filter = g },
                    count = all.count { !it.read && it.type.group == g }, modifier = Modifier.enterPop(i + 1),
                )
            }
        }

        // 필터를 바꾸면 목록이 부드럽게 바뀐다
        AnimatedContent(
            targetState = shown.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "notiEmpty",
        ) { empty ->
            if (empty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.enterPop(0, from = 0.8f)) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .background(Aqua100, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) { MBIcon(R.drawable.ic_mb_bell, size = 30.dp, tint = Aqua700) }
                        Spacer(Modifier.height(16.dp))
                        Text("아직 알림이 없어요", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Black)
                        Spacer(Modifier.height(4.dp))
                        Text("투표 요청이나 일정 변경이 생기면 여기에 모여요.", fontSize = 13.sp, color = Sub)
                    }
                }
            } else {
                NotificationList(shown, onClick = { n -> repository.markRead(n.id); onOpen(n) })
            }
        }
    }
    }
}

/** 알림 맨 위 '친구 신청' 묶음: 사진 · 이름 · [수락] [거절] */
@Composable
private fun FriendRequestSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val requests by remember { FriendRequests.observeIncoming() }.collectAsState(initial = emptyList())
    var busy by remember { mutableStateOf<String?>(null) }
    androidx.compose.animation.AnimatedVisibility(visible = requests.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                "친구 신청 ${requests.size}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Aqua700,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
            )
            androidx.compose.material3.HorizontalDivider(color = com.example.mbtrip.ui.theme.Line)
            requests.forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.example.mbtrip.timeline.ui.MemberAvatar(r.from, size = 44.dp)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${r.from.nickname} 님이 친구 신청을 보냈어요", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Black)
                        Text(r.from.handle, fontSize = 12.sp, color = Sub)
                    }
                    val working = busy == r.id
                    RequestButton("거절", filled = false, enabled = !working) {
                        scope.launch {
                            busy = r.id
                            FriendRequests.decline(r)?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
                            busy = null
                        }
                    }
                    Spacer(Modifier.size(6.dp))
                    RequestButton(if (working) "…" else "수락", filled = true, enabled = !working) {
                        scope.launch {
                            busy = r.id
                            val problem = FriendRequests.accept(r)
                            android.widget.Toast.makeText(
                                context, problem ?: "${r.from.nickname} 님과 친구가 됐어요.", android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            busy = null
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(color = com.example.mbtrip.ui.theme.Line)
            }
        }
    }
}

@Composable
private fun RequestButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (filled) Color.White else Black,
        modifier = Modifier
            .bouncyClick(shape, enabled = enabled, onClick = onClick)
            .background(if (filled) Black else Color.White, shape)
            .border(1.dp, if (filled) Black else com.example.mbtrip.ui.theme.Line, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun NotificationList(shown: List<AppNotification>, onClick: (AppNotification) -> Unit) {
        val today = LocalDate.now()
        val grouped = shown.groupBy { dayLabel(it.createdAt.toLocalDate(), today) }
        LazyColumn(Modifier.fillMaxSize()) {
            grouped.forEach { (label, list) ->
                item(key = "h_$label") {
                    Text(
                        label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Aqua700,
                        modifier = Modifier.animateItem().padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(list, key = { _, n -> n.id }) { i, n ->
                    // 처음엔 차례로 떠오르고, 읽음·필터로 바뀌면 자리 이동이 부드럽게
                    Box(Modifier.animateItem().enterFromBelow(i, distance = 20.dp, delayStepMs = 50)) {
                        NotificationRow(n) { onClick(n) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val bg by animateColorAsState(if (n.read) Color.White else UnreadBg, label = "notiBg")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .bouncyClick(RoundedCornerShape(20.dp), scaleDown = 0.97f, elevation = if (n.read) 2.dp else 5.dp, shadowColor = Aqua500, onClick = onClick)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val icon = NotificationIcons.of(n.type)
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(icon.background)
                .border(1.dp, icon.border, CircleShape), // 얇은 테두리
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
                MBIcon(R.drawable.ic_mb_bell, size = 20.dp, tint = Ink)
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
                n.body, fontSize = 13.sp, color = if (n.read) Sub else InkSoft, lineHeight = 18.sp,
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
