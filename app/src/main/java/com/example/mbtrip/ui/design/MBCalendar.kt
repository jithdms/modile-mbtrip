package com.example.mbtrip.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.R
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Coral
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Field
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * ───────────── MBTrip 공용 달력 ─────────────
 * 앱의 모든 달력(방 만들기 기간 · 숙소·교통 날짜 · 여행 로그 기간 · 마이페이지)이 이 달력 하나를 쓴다.
 *  - MBMonthCalendar : 한 달 칸 (기간이면 시작~끝 사이에 연한 띠)
 *  - MBCalendarSheet : 아래에서 올라오는 모달 껍데기 (손잡이 · 제목 · 닫기 · 아래 버튼)
 *  - MBDateRangeSheet: 달력 하나에서 시작일 → 끝날을 차례로 누르는 기간 선택 모달
 */

private val WEEK = listOf("일", "월", "화", "수", "목", "금", "토")
private val DOT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** 2026.10.01 (목) */
fun LocalDate.mbDateText(): String =
    "${format(DOT_DATE)} (${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

/**
 * 한 달 달력.
 * @param start  고른 날 (하나만 고를 땐 이것만)
 * @param end    기간의 끝날 (null = 하나만 / 아직 안 고름)
 * @param marked 같이 표시할 날짜들 (예: 내 여행 날짜) — 하늘색 띠 + 굵은 숫자
 * @param onDayClick null 이면 보기 전용
 */
@Composable
fun MBMonthCalendar(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    start: LocalDate?,
    end: LocalDate?,
    onDayClick: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    marked: Set<LocalDate> = emptySet(),
) {
    val today = remember { LocalDate.now() }
    Column(modifier.fillMaxWidth()) {
        // ‹ 2026년 10월 ›
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(R.drawable.ic_mb_chevron_left, "이전 달") { onMonthChange(month.minusMonths(1)) }
            AnimatedContent(
                targetState = month,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "calTitle",
            ) { m ->
                Text(
                    "${m.year}년 ${m.monthValue}월",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            MonthArrow(R.drawable.ic_mb_chevron_right, "다음 달") { onMonthChange(month.plusMonths(1)) }
        }
        Spacer(Modifier.height(10.dp))
        // 요일
        Row(Modifier.fillMaxWidth()) {
            WEEK.forEachIndexed { i, d ->
                Text(
                    d, fontSize = 12.sp, textAlign = TextAlign.Center,
                    color = when (i) { 0 -> Coral; 6 -> Aqua700; else -> Muted },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 날짜 칸 — 달을 넘기면 옆으로 밀리며 바뀐다
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val next = targetState > initialState
                (slideInHorizontally { if (next) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (next) -it / 4 else it / 4 } + fadeOut())
            },
            label = "calGrid",
        ) { m ->
            Column {
                cells(m).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEachIndexed { i, day ->
                            DayCell(
                                day = day,
                                weekday = i,
                                start = start,
                                end = end,
                                marked = day != null && day in marked,
                                today = today,
                                enabled = day != null && (minDate == null || !day.isBefore(minDate)) && (maxDate == null || !day.isAfter(maxDate)),
                                onClick = onDayClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(icon: Int, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).bouncyClick(CircleShape, onClick = onClick).border(Hairline, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { MBIcon(icon, size = 20.dp, tint = Ink, contentDescription = label) }
}

/** 한 칸: 기간 사이는 연한 띠, 시작·끝은 아쿠아 동그라미, 오늘은 아래 점 */
@Composable
private fun DayCell(
    day: LocalDate?,
    weekday: Int,
    start: LocalDate?,
    end: LocalDate?,
    marked: Boolean,
    today: LocalDate,
    enabled: Boolean,
    onClick: ((LocalDate) -> Unit)?,
    modifier: Modifier,
) {
    Box(modifier.height(44.dp), contentAlignment = Alignment.Center) {
        if (day == null) return@Box
        val isStart = day == start
        val isEnd = day == end
        val selected = isStart || isEnd
        val hasRange = start != null && end != null && end.isAfter(start)
        val inRange = hasRange && !day.isBefore(start) && !day.isAfter(end)
        // 기간 띠 (시작 칸은 오른쪽 반, 끝 칸은 왼쪽 반만)
        if (inRange || marked) {
            val band = Aqua100
            Row(Modifier.fillMaxWidth().height(36.dp)) {
                Box(Modifier.weight(1f).height(36.dp).background(if (inRange && isStart) Color.Transparent else band))
                Box(Modifier.weight(1f).height(36.dp).background(if (inRange && isEnd) Color.Transparent else band))
            }
            if (inRange && (isStart || isEnd)) Box(Modifier.size(36.dp).background(band, CircleShape))
        }
        Box(
            Modifier
                .size(36.dp)
                .then(
                    if (onClick != null && enabled) Modifier.bouncyClick(CircleShape) { onClick(day) } else Modifier,
                )
                .then(if (selected) Modifier.background(Aqua300, CircleShape).border(Hairline, Aqua500, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.dayOfMonth}",
                fontSize = 14.sp,
                fontWeight = if (selected || marked) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !enabled -> Faint
                    selected -> Ink
                    weekday == 0 -> Coral
                    weekday == 6 -> Aqua700
                    else -> Ink
                },
            )
            if (day == today && !selected) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).background(Aqua500, CircleShape))
            }
        }
    }
}

/** 그 달의 칸 (앞쪽 빈칸 null 포함, 일요일 시작) */
private fun cells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value % 7 // 일=0
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val all: List<LocalDate?> = List(lead) { null } + days
    return all + List((7 - all.size % 7) % 7) { null }
}

/* ───────────────────────── 모달 ───────────────────────── */

/** 아래에서 올라오는 달력 모달 껍데기 — 모든 날짜 선택 화면이 같은 모양 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MBCalendarSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Line),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).bouncyClick(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_close, size = 18.dp, tint = Muted, contentDescription = "닫기") }
            }
            Spacer(Modifier.height(14.dp))
            content()
            Spacer(Modifier.height(16.dp))
            MBButton(text = confirmText, onClick = onConfirm, enabled = confirmEnabled, style = MBButtonStyle.Deep)
        }
    }
}

/**
 * 기간 선택 모달 — 달력 하나에서 시작일을 누르고, 이어서 끝날을 누른다.
 *  - 시작일보다 앞을 누르면 그 날이 새 시작일
 *  - 둘 다 고른 뒤 다시 누르면 처음부터
 *  - 끝날을 안 고르고 완료하면 하루(당일)
 */
@Composable
fun MBDateRangeSheet(
    title: String,
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
    confirmText: String = "완료",
    startLabel: String = "시작",
    endLabel: String = "끝",
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var month by remember { mutableStateOf(YearMonth.from(initialStart ?: LocalDate.now())) }
    MBCalendarSheet(
        title = title,
        onDismiss = onDismiss,
        confirmText = confirmText,
        confirmEnabled = start != null,
        onConfirm = { start?.let { s -> onConfirm(s, end ?: s) } },
    ) {
        // 고른 값 두 칸 — 다음에 누를 칸이 진하게
        val choosingEnd = start != null && end == null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickedBox(startLabel, start?.mbDateText(), active = !choosingEnd, modifier = Modifier.weight(1f))
            PickedBox(endLabel, end?.mbDateText(), active = choosingEnd, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        val s0 = start
        val e0 = end
        Text(
            when {
                s0 == null -> "$startLabel 날짜를 눌러 주세요"
                e0 == null -> "$endLabel 날짜를 눌러 주세요 (하루면 그대로 완료)"
                else -> {
                    val nights = java.time.temporal.ChronoUnit.DAYS.between(s0, e0)
                    if (nights == 0L) "당일" else "${nights}박 ${nights + 1}일"
                }
            },
            fontSize = 12.sp,
            color = Muted,
        )
        Spacer(Modifier.height(14.dp))
        MBMonthCalendar(
            month = month,
            onMonthChange = { month = it },
            start = start,
            end = end,
            minDate = minDate,
            maxDate = maxDate,
            onDayClick = { d ->
                val s = start
                when {
                    s == null || end != null -> { start = d; end = null }
                    d.isBefore(s) -> start = d
                    else -> end = d
                }
            },
        )
    }
}

@Composable
private fun PickedBox(label: String, value: String?, active: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .background(if (active) Aqua100 else Color.White, shape)
            .border(Hairline, if (active) Aqua500 else Line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(2.dp))
        Text(value ?: "선택", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (value != null) Ink else Faint, maxLines = 1)
    }
}

