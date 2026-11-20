package com.example.mbtrip.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.R
import com.example.mbtrip.siheung.PlaceLocator
import com.example.mbtrip.siheung.RoutineStop
import com.example.mbtrip.siheung.TourRoutine
import com.example.mbtrip.siheung.TourRoutines
import com.example.mbtrip.timeline.ui.MapSpot
import com.example.mbtrip.timeline.ui.PlaceMap
import com.example.mbtrip.timeline.ui.composeColor
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBChip
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.theme.Aqua50
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import java.time.Duration
import java.time.LocalTime

/*
 * 방 만들기 아래 '추천 투어 루틴'.
 * 카드를 누르면 미리보기(지도 + 순서) → [이 순서로 돌아다니기] 를 누르면
 * 여행이 만들어지고, 일정표를 열 때 이 순서대로 블록이 들어간다.
 */

@Composable
fun TourRoutineSection(
    /** [이 순서로 돌아다니기] */
    onApply: (TourRoutine) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewing by remember { mutableStateOf<TourRoutine?>(null) }
    Column(modifier.fillMaxWidth()) {
        SectionLabel("추천 투어 루틴", icon = R.drawable.ic_mb_compass)
        Spacer(Modifier.height(4.dp))
        Text("눌러서 미리 보고, 마음에 들면 그 순서대로 일정표를 채워 드려요.", fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TourRoutines.all.forEachIndexed { i, r ->
                RoutineCard(r, Modifier.enterFromBelow(i, distance = 16.dp)) { previewing = r }
            }
        }
    }
    previewing?.let { r ->
        RoutinePreviewDialog(
            routine = r,
            onDismiss = { previewing = null },
            onApply = {
                previewing = null
                onApply(r)
            },
        )
    }
}

@Composable
private fun RoutineCard(routine: TourRoutine, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .width(210.dp)
            .bouncyClick(shape, onClick = onClick)
            .background(Color.White, shape)
            .border(Hairline, Line, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftBadge(routine.lengthLabel)
            Spacer(Modifier.width(6.dp))
            Text("${routine.stopCount}곳", fontSize = 12.sp, color = Muted)
        }
        Spacer(Modifier.height(8.dp))
        Text(routine.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(routine.summary, fontSize = 12.sp, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        // 첫 4곳 — 점을 세로 선으로 이어서
        val firstDay = routine.days.first()
        val shown = firstDay.take(4)
        shown.forEachIndexed { i, st ->
            Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(9.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    // 위아래 선 (첫 칸은 아래만, 마지막 칸은 위만)
                    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(1.5.dp).weight(1f).background(if (i == 0) Color.Transparent else RouteLine))
                        Box(Modifier.width(1.5.dp).weight(1f).background(if (i == shown.lastIndex && firstDay.size <= 4) Color.Transparent else RouteLine))
                    }
                    Box(Modifier.size(9.dp).background(Color.White, CircleShape).padding(1.5.dp).background(st.place?.category?.composeColor ?: Muted, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${st.start.hm()}  ${st.placeName}",
                    fontSize = 12.sp, color = if (i == 0) Ink else InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
        if (firstDay.size > 4) {
            Text("⋯ 외 ${routine.stopCount - 4}곳", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(start = 17.dp, top = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("미리보기", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Aqua700)
            MBIcon(R.drawable.ic_mb_chevron_right, size = 16.dp, tint = Aqua700)
        }
    }
}

/** 루틴 미리보기: 위 지도(순서대로 선으로 이은 장소) + 아래 시간 순서(세로 선) + 맨 아래 버튼 */
@Composable
private fun RoutinePreviewDialog(routine: TourRoutine, onDismiss: () -> Unit, onApply: () -> Unit) {
    val context = LocalContext.current
    var day by remember(routine.id) { mutableIntStateOf(0) }
    val stops = routine.days[day]

    // 이 루틴의 장소 좌표 찾기 (한 번 찾으면 폰에 저장)
    LaunchedEffect(routine.id) {
        runCatching {
            PlaceLocator.locateAll(context.applicationContext, routine.days.flatten().mapNotNull { it.place })
        }
    }
    val locatedCount = PlaceLocator.located.size
    val spots = remember(routine.id, day, locatedCount) {
        stops.mapIndexedNotNull { i, st ->
            val ll = st.place?.let { PlaceLocator.of(it) } ?: return@mapIndexedNotNull null
            MapSpot(ll.lat, ll.lng, "${i + 1}. ${st.placeName}")
        }
    }
    val route = remember(spots) { spots.map { it.lat to it.lng } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .fillMaxHeight(0.86f)
                .enterPop(0, from = 0.96f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
        ) {
            // ① 머리
            Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftBadge(routine.lengthLabel)
                        Spacer(Modifier.width(6.dp))
                        Text("${routine.stopCount}곳 · ${routine.mood}", fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(routine.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(routine.summary, fontSize = 13.sp, color = InkSoft)
                }
                Box(
                    Modifier.size(40.dp).bouncyClick(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_close, size = 18.dp, tint = Ink, contentDescription = "닫기") }
            }

            // ② 지도 — 장소를 순서대로 선으로
            Box(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(Hairline, Line, RoundedCornerShape(14.dp))
                    .background(Background),
            ) {
                PlaceMap(
                    lat = null,
                    lng = null,
                    enabled = false,
                    onPick = { _, _ -> },
                    spots = spots,
                    route = route,
                    dimOutside = false,
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                )
                if (spots.isEmpty()) {
                    Text(
                        "장소 위치를 찾는 중…",
                        fontSize = 12.sp, color = Muted,
                        modifier = Modifier.align(Alignment.Center).background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (routine.days.size > 1) {
                Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    routine.days.indices.forEach { i ->
                        MBChip("${i + 1}일차", selected = i == day, onClick = { day = i })
                    }
                }
            }

            // ③ 순서 — 남는 높이를 다 쓰고 안에서 스크롤
            AnimatedContent(
                targetState = day,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "routineDay",
            ) { d ->
                val list = routine.days[d]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    list.forEachIndexed { i, st ->
                        StopRow(i + 1, st, isFirst = i == 0, isLast = i == list.lastIndex, next = list.getOrNull(i + 1))
                    }
                }
            }

            // ④ 버튼
            HorizontalDivider(color = Line)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                MBButton(
                    text = "이 순서로 돌아다니기",
                    onClick = onApply,
                    style = MBButtonStyle.Deep,
                    leadingIcon = R.drawable.ic_mb_arrow_right,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "지금 고른 날짜·인원으로 시흥 여행 방을 만들고, 일정표에 이 순서대로 넣어 드려요.",
                    fontSize = 11.sp, color = Muted,
                )
            }
        }
    }
}

/** 세로 선으로 이어진 한 곳: 시간 | ●(번호) | 이름 · 종류 · 머무는 시간 · 팁 / 다음 곳까지 이동 */
@Composable
private fun StopRow(number: Int, stop: RoutineStop, isFirst: Boolean, isLast: Boolean, next: RoutineStop?) {
    val color = stop.place?.category?.composeColor ?: Muted
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(stop.start.hm(), fontSize = 12.sp, color = Muted, modifier = Modifier.width(44.dp).padding(top = 3.dp))
        // 선 + 번호 점 (선이 칸 높이만큼 쭉 이어진다)
        Box(Modifier.width(24.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxHeight().align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(2.dp).height(4.dp).background(if (isFirst) Color.Transparent else RouteLine))
                Spacer(Modifier.height(20.dp))
                Box(Modifier.width(2.dp).weight(1f).background(if (isLast) Color.Transparent else RouteLine))
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(22.dp).background(color, CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$number", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp, bottom = if (isLast) 8.dp else 16.dp)) {
            Text(stop.placeName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            val sub = listOfNotNull(
                stop.place?.category?.label,
                "${Duration.between(stop.start, stop.end).toMinutes()}분",
                stop.place?.note,
            ).joinToString(" · ")
            Text(sub, fontSize = 12.sp, color = Muted)
            stop.tip?.let { Text(it, fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp)) }
            if (next != null) {
                val gap = Duration.between(stop.end, next.start).toMinutes()
                if (gap >= 15) {
                    Text(
                        "이동 약 ${gap}분",
                        fontSize = 11.sp, color = Aqua700, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp).background(Aqua50, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** 루틴 선 색 */
private val RouteLine = Color(0xFF26A6C6)

private fun LocalTime.hm(): String = "%02d:%02d".format(hour, minute)
