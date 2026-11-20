package com.example.mbtrip.createtrip

import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.R
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.drawBehind
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBCalendarSheet
import com.example.mbtrip.ui.design.MBMonthCalendar
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua200
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Coral
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

private val Black = Ink
private val Gray = Muted
private val LightGray = Aqua100       // 고른 값 띠 · 같이 보여줄 날짜
private val Disabled = Line
private val SunRed = Coral
private val SatBlue = Aqua700
private val TodaySky = Aqua300        // 브랜드 하늘색

internal val ItemH = 40.dp // 휠 한 줄 높이 (3줄이 보임)

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
    val startDate = clampDate(initial.toLocalDate(), minDate, maxDate)
    var date by remember { mutableStateOf(startDate) }
    var month by remember { mutableStateOf(YearMonth.from(startDate)) }
    val (initPm, initH12) = to12h(initial.hour)
    var pm by remember { mutableStateOf(initPm) }
    var hour12 by remember { mutableIntStateOf(initH12) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    val picked = LocalDateTime.of(date, LocalTime.of(from12h(pm, hour12), minute))

    // 앱 공용 달력 모달 (방 만들기·여행 로그와 같은 모양)
    MBCalendarSheet(
        title = title,
        onDismiss = onDismiss,
        confirmText = "완료",
        confirmEnabled = true,
        onConfirm = { onConfirm(picked) },
    ) {
        // ── 달력 (하루 고르기) ──
        MBMonthCalendar(
            month = month,
            onMonthChange = { month = it },
            start = date,
            end = null,
            minDate = minDate,
            maxDate = maxDate,
            marked = setOfNotNull(markedDate),
            onDayClick = { date = it },
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line)

        // ── 시간 휠 ──
        Box(Modifier.fillMaxWidth().height(ItemH * 3), contentAlignment = Alignment.Center) {
            // 가운데 줄 = 선택값 표시 띠
            Box(
                Modifier.fillMaxWidth().height(ItemH)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Aqua100),
            )
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Wheel(listOf("오전", "오후"), if (pm) 1 else 0, Modifier.width(72.dp)) { pm = it == 1 }
                Wheel((1..12).map { "$it" }, hour12 - 1, Modifier.width(64.dp)) { hour12 = it + 1 }
                Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Wheel((0..59).map { "%02d".format(it) }, minute, Modifier.width(64.dp)) { minute = it }
            }
        }

        Spacer(Modifier.height(12.dp))
        // 고른 날짜·시간
        Text(
            previewText(picked),
            fontSize = 14.sp, color = Aqua700, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
internal fun Wheel(items: List<String>, initialIndex: Int, modifier: Modifier, onSelected: (Int) -> Unit) {
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
