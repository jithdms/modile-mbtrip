package com.example.mbtrip.createtrip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

private val Black = Color(0xFF1E1E1E)
private val Gray = Color(0xFF9E9E9E)
private val LightGray = Color(0xFFEDEDED)
private val Disabled = Color(0xFFD6D6D6)
private val SunRed = Color(0xFFE53935)
private val SatBlue = Color(0xFF1E88E5)
private val TodaySky = Color(0xFFAED6F1) // 일정표 확정 블록 하늘색

private val ItemH = 40.dp // 휠 한 줄 높이 (3줄이 보임)

/**
 * 날짜·시간 선택 시트: 위 = 달력, 아래 = 시간 휠(오전/오후 · 시 · 분 1분 단위), [완료].
 *
 * @param initial    처음 보여줄 값
 * @param minDate, maxDate 고를 수 있는 기간 (null = 제한 없음). 밖의 날짜는 회색, 못 누름
 * @param markedDate 같이 보여줄 날짜 (예: 체크아웃 고를 때 체크인 날) — 연한 회색 동그라미
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimePickerSheet(
    title: String,
    initial: LocalDateTime,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    markedDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val startDate = clampDate(initial.toLocalDate(), minDate, maxDate)
    var date by remember { mutableStateOf(startDate) }
    var month by remember { mutableStateOf(YearMonth.from(startDate)) }
    val (initPm, initH12) = to12h(initial.hour)
    var pm by remember { mutableStateOf(initPm) }
    var hour12 by remember { mutableIntStateOf(initH12) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    val picked = LocalDateTime.of(date, LocalTime.of(from12h(pm, hour12), minute))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            // ── 제목 + ✕ ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 18.sp, color = Gray) }
            }
            Spacer(Modifier.height(12.dp))

            // ── 달력 ──
            MonthHeader(month, onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) })
            Spacer(Modifier.height(8.dp))
            CalendarGrid(
                month = month,
                selected = date,
                marked = markedDate,
                minDate = minDate,
                maxDate = maxDate,
                onPick = { date = it },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LightGray)

            // ── 시간 휠 ──
            Box(Modifier.fillMaxWidth().height(ItemH * 3), contentAlignment = Alignment.Center) {
                // 가운데 줄 = 선택값 표시 띠
                Box(
                    Modifier.fillMaxWidth().height(ItemH)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightGray),
                )
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Wheel(listOf("오전", "오후"), if (pm) 1 else 0, Modifier.width(72.dp)) { pm = it == 1 }
                    Wheel((1..12).map { "$it" }, hour12 - 1, Modifier.width(64.dp)) { hour12 = it + 1 }
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Wheel((0..59).map { "%02d".format(it) }, minute, Modifier.width(64.dp)) { minute = it }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                previewText(picked),
                fontSize = 15.sp, color = Black, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Black)
                    .clickable { onConfirm(picked) },
                contentAlignment = Alignment.Center,
            ) { Text("완료", color = Color.White, fontSize = 16.sp) }
        }
    }
}

/* ───────────── 달력 ───────────── */

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowCell("‹", onPrev)
        Text(
            "${month.year}년 ${month.monthValue}월",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        ArrowCell("›", onNext)
    }
}

@Composable
private fun ArrowCell(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 24.sp, color = Black) }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selected: LocalDate,
    marked: LocalDate?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onPick: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    // 요일 줄
    Row(Modifier.fillMaxWidth()) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, d ->
            Text(
                d, fontSize = 12.sp, textAlign = TextAlign.Center,
                color = when (i) { 0 -> SunRed; 6 -> SatBlue; else -> Gray },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    monthCells(month).chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                    if (day != null) {
                        DayCell(
                            day = day,
                            isSelected = day == selected,
                            isMarked = day == marked,
                            isToday = day == today,
                            enabled = isSelectable(day, minDate, maxDate),
                            onClick = { onPick(day) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isMarked: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val textColor = when {
        isSelected -> Color.White
        !enabled -> Disabled
        day.isSunday -> SunRed
        day.isSaturday -> SatBlue
        else -> Black
    }
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> Black
                    isMarked -> LightGray
                    else -> Color.Transparent
                },
            )
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, TodaySky, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.dayOfMonth}",
            fontSize = 15.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/* ───────────── 휠 (위아래 스크롤, 가운데 줄이 선택값) ───────────── */

/**
 * 목록 앞뒤에 빈 줄을 하나씩 넣어서, 맨 위에 보이는 줄 번호 = 가운데 값 번호가 되게 한다.
 * 손을 떼면 한 줄 단위로 딱 맞춰 멈춘다(snap).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Wheel(items: List<String>, initialIndex: Int, modifier: Modifier, onSelected: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { ItemH.toPx() }

    val center by remember {
        derivedStateOf {
            val i = state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            i.coerceIn(0, items.lastIndex)
        }
    }
    LaunchedEffect(center) { onSelected(center) }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.height(ItemH * 3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Spacer(Modifier.height(ItemH)) }
        itemsIndexed(items) { i, label ->
            val isCenter = i == center
            Box(Modifier.height(ItemH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    label,
                    fontSize = if (isCenter) 20.sp else 17.sp,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCenter) Black else Gray,
                )
            }
        }
        item { Spacer(Modifier.height(ItemH)) }
    }
}
