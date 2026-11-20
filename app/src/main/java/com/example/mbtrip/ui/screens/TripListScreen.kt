package com.example.mbtrip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.mbtrip.session.TripSession
import com.example.mbtrip.session.TripRooms
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.ui.theme.Danger
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.R
import com.example.mbtrip.session.MyTrip
import com.example.mbtrip.session.MyTrips
import com.example.mbtrip.session.TestAccess
import com.example.mbtrip.ui.component.BottomTab
import com.example.mbtrip.ui.component.MBBottomBar
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Field
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 내 여행 — 이 폰에서 만들었거나 초대코드로 들어간 여행 방 목록.
 * 하나를 누르면 그 여행이 '지금 여행'이 되고 일정표로 간다.
 * 다가오는 여행 / 지난 여행 으로 나눠 보여준다.
 */
@Composable
fun TripListScreen(
    onBack: () -> Unit,
    onOpenTrip: (MyTrip) -> Unit,
    onCreateTrip: () -> Unit,
    onTab: (BottomTab) -> Unit,
    hasUnreadNotifications: Boolean = false,
) {
    // 열 때마다 Firestore 에서 날짜·목적지를 새로 맞춘다 (실패해도 기억해 둔 값으로 보여줌)
    LaunchedEffect(Unit) { runCatching { MyTrips.refresh() } }
    val trips = MyTrips.trips
    // 테스트 계정은 체험용 여행이 맨 위에 따로 보인다
    val demo = if (TestAccess.isTester) TestAccess.demoTrips else emptyList()
    val today = LocalDate.now()
    val upcoming = trips.filter { (it.endDate ?: it.startDate)?.isBefore(today) != true }
    val past = trips.filter { (it.endDate ?: it.startDate)?.isBefore(today) == true }
    // 지우려고 고른 여행 → 확인 창
    var deleting by remember { mutableStateOf<MyTrip?>(null) }
    var removingRoom by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(Background)) {
        ScreenHeader(title = "내 여행", subtitle = "만든 방과 초대받은 방 ${trips.size}개", onBack = onBack)
        AnimatedContent(
            targetState = trips.isEmpty() && demo.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "tripsEmpty",
        ) { empty ->
            if (empty) {
                EmptyTrips(onCreateTrip)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (demo.isNotEmpty()) {
                        item(key = "h_demo") { GroupTitle("테스트 자료", demo.size) }
                        itemsIndexed(demo, key = { _, t -> "d_" + t.tripId }) { i, t ->
                            TripCard(t, today, Modifier.animateItem().enterFromBelow(i), onDelete = null) { onOpenTrip(t) }
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item(key = "h_up") { GroupTitle("다가오는 여행", upcoming.size, top = if (demo.isEmpty()) 0.dp else 12.dp) }
                        itemsIndexed(upcoming, key = { _, t -> "u_" + t.tripId }) { i, t ->
                            TripCard(t, today, Modifier.animateItem().enterFromBelow(i), onDelete = { deleting = t }) { onOpenTrip(t) }
                        }
                    }
                    if (past.isNotEmpty()) {
                        item(key = "h_past") { GroupTitle("지난 여행", past.size, top = if (upcoming.isEmpty() && demo.isEmpty()) 0.dp else 12.dp) }
                        itemsIndexed(past, key = { _, t -> "p_" + t.tripId }) { i, t ->
                            TripCard(t, today, Modifier.animateItem().enterFromBelow(i), onDelete = { deleting = t }) { onOpenTrip(t) }
                        }
                    }
                    item(key = "new") {
                        Spacer(Modifier.height(6.dp))
                        MBButton(
                            text = "새 여행 만들기",
                            onClick = onCreateTrip,
                            style = MBButtonStyle.Outline,
                            leadingIcon = R.drawable.ic_mb_add,
                            height = 48.dp,
                        )
                    }
                }
            }
        }
        MBBottomBar(selected = BottomTab.MyTrips, onSelect = onTab, hasUnreadNotifications = hasUnreadNotifications)
    }

    deleting?.let { t ->
        val name = t.destination.ifBlank { "여행" }
        // 나만 빠지기 (초대받은 방 · 내가 만든 방 둘 다)
        fun leave() {
            deleting = null
            MyTrips.forget(t.tripId)
            if (TripSession.tripId == t.tripId) TripSession.clear()
            scope.launch {
                val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (me != null) runCatching { TimelineDependencies.memberRepository.remove(t.tripId, me) }
            }
            Toast.makeText(context, "'$name' 방에서 나왔어요.", Toast.LENGTH_SHORT).show()
        }
        AlertDialog(
            onDismissRequest = { if (!removingRoom) deleting = null },
            title = { Text(if (t.createdByMe) "'$name' 방을 어떻게 할까요?" else "'$name' 방에서 나갈까요?") },
            text = {
                Text(
                    if (t.createdByMe) {
                        "방 삭제: 일정 · 동행자 · 후기가 모두 지워지고, 함께하던 친구들의 목록에서도 사라져요. 되돌릴 수 없어요.\n\n" +
                            "나만 나가기: 내 목록에서만 빠지고, 친구들은 계속 이 방을 쓸 수 있어요."
                    } else {
                        "내 여행 목록에서 빠지고 동행자에서도 나가요. 다시 들어가려면 초대코드가 필요해요."
                    },
                )
            },
            confirmButton = {
                if (t.createdByMe) {
                    Row {
                        TextButton(onClick = { leave() }, enabled = !removingRoom) { Text("나만 나가기") }
                        TextButton(
                            enabled = !removingRoom,
                            onClick = {
                                removingRoom = true
                                scope.launch {
                                    val problem = TripRooms.deleteRoom(t.tripId)
                                    removingRoom = false
                                    deleting = null
                                    Toast.makeText(context, problem ?: "'$name' 방을 삭제했어요.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) { Text(if (removingRoom) "삭제하는 중…" else "방 삭제", color = Danger, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    TextButton(onClick = { leave() }) { Text("나가기", color = Danger) }
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }, enabled = !removingRoom) { Text("취소") } },
        )
    }
}

@Composable
private fun GroupTitle(text: String, count: Int, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Row(Modifier.padding(top = top, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Ink)
        Spacer(Modifier.width(6.dp))
        Text("$count", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Aqua700)
    }
}

/** 여행 한 장: 목적지 · 출발지 → 목적지 · 날짜 · 인원 · 방 종류 */
@Composable
private fun TripCard(trip: MyTrip, today: LocalDate, modifier: Modifier, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val start = trip.startDate
    val end = trip.endDate ?: start
    val isPast = (end ?: start)?.isBefore(today) == true
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClick(shape, onClick = onClick)
            .background(Color.White, shape)
            .border(Hairline, Line, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPast) Field else Aqua100),
            contentAlignment = Alignment.Center,
        ) { MBIcon(R.drawable.ic_mb_flight, size = 22.dp, tint = if (isPast) Muted else Aqua700) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    trip.destination.ifBlank { "여행" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                SoftBadge(dDayText(start, end, today))
            }
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    if (start != null) {
                        append(start.format(DATE))
                        if (end != null && end != start) append(" ~ ${end.format(DATE)}")
                        val nights = if (end != null) ChronoUnit.DAYS.between(start, end) else 0L
                        append(if (nights <= 0L) " · 당일" else " · ${nights}박 ${nights + 1}일")
                    } else {
                        append("날짜 미정")
                    }
                },
                fontSize = 13.sp,
                color = InkSoft,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    trip.departure.takeIf { it.isNotBlank() }?.let { "$it 출발" },
                    trip.headcount?.let { "${it}명" },
                    if (trip.createdByMe) "내가 만든 방" else "초대받은 방",
                ).joinToString(" · "),
                fontSize = 12.sp,
                color = Muted,
            )
        }
        Spacer(Modifier.width(4.dp))
        if (onDelete != null) {
            // 휴지통 — 누르면 확인 창
            Box(
                Modifier.size(40.dp).bouncyClick(CircleShape, onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) { MBIcon(R.drawable.ic_mb_delete, size = 20.dp, tint = Muted, contentDescription = "여행 삭제") }
        } else {
            MBIcon(R.drawable.ic_mb_arrow_right, size = 18.dp, tint = Muted)
        }
    }
}

/** D-3 · 여행 중 · 지난 여행 */
private fun dDayText(start: LocalDate?, end: LocalDate?, today: LocalDate): String {
    if (start == null) return "미정"
    val last = end ?: start
    return when {
        today.isBefore(start) -> "D-${ChronoUnit.DAYS.between(today, start)}"
        !today.isAfter(last) -> "여행 중"
        else -> "다녀옴"
    }
}

@Composable
private fun EmptyTrips(onCreateTrip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).enterFromBelow(0),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Aqua100),
            contentAlignment = Alignment.Center,
        ) { MBIcon(R.drawable.ic_mb_luggage, size = 30.dp, tint = Aqua700) }
        Spacer(Modifier.height(16.dp))
        Text("아직 여행 방이 없어요", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("방을 만들거나 초대코드로 들어가면 여기에 모여요.", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(20.dp))
        MBButton(text = "새 여행 만들기", onClick = onCreateTrip, style = MBButtonStyle.Brand, height = 48.dp)
    }
}
