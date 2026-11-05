package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockDisplay
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.MarkerType
import com.example.mbtrip.timeline.model.TimeMarker
import java.time.LocalTime
import kotlin.math.roundToInt

/* ───────────────────────── 색 규칙 (디자인 시스템 확정 전 임시값) ───────────────────────── */

internal object TimelineColors {
    val Move = Color(0xFF111111)                         // 검정: 이동
    val Fixed = Color(0xFFAED6F1)                        // 하늘: 확정
    val Free = Color(0xFFA9E5A0)                         // 초록: 자유
    val FixedPending = lerp(Fixed, Color.White, 0.55f)   // 연한 하늘: 제안됨·투표중 (반투명 흰색을 덮은 효과)
    val FreeEmpty = lerp(Free, Color.White, 0.6f)        // 연한 초록: 빈칸
    val FixedDash = Color(0xFF5DADE2)
    val FreeDash = Color(0xFF6CBF62)
    val Border = Color(0xFF9E9E9E)
    val Axis = Color(0xFFBDBDBD)
    val GridLine = Color(0xFFEEEEEE)
    val NowLine = Color(0xFFD32F2F)                      // 빨강: 현재 시각
    val Marker = Color(0xFF1E88E5)                       // 파랑: 체크인·체크아웃
    val SubText = Color(0xFF757575)
    val Error = Color(0xFFD32F2F)
}

internal data class CardStyle(
    val bg: Color, val fg: Color, val border: Color, val dashed: Boolean, val label: String,
)

internal fun Block.cardStyle(): CardStyle = when (display) {
    BlockDisplay.MOVE -> CardStyle(TimelineColors.Move, Color.White, TimelineColors.Move, false, "이동")
    BlockDisplay.FIXED -> CardStyle(TimelineColors.Fixed, Color.Black, TimelineColors.Border, false, "확정")
    BlockDisplay.FIXED_PENDING -> CardStyle(
        TimelineColors.FixedPending, Color.Black, TimelineColors.FixedDash, true,
        if (status == BlockStatus.VOTING) "투표중" else "제안됨",
    )
    BlockDisplay.FREE -> CardStyle(TimelineColors.Free, Color.Black, TimelineColors.Border, false, "자유")
    BlockDisplay.FREE_EMPTY -> CardStyle(TimelineColors.FreeEmpty, Color(0xFF4F7F4A), TimelineColors.FreeDash, true, "자유")
}

/** 실선/점선 테두리 */
internal fun Modifier.blockBorder(color: Color, dashed: Boolean, radius: Dp = 10.dp): Modifier = drawBehind {
    val stroke = 1.5.dp.toPx()
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(
            width = stroke,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())) else null,
        ),
    )
}

/* ───────────────────────── 시간축 타임라인 ───────────────────────── */

internal val HOUR_HEIGHT = 96.dp        // 1시간 = 96dp, 30분 = 48dp, 최소 블록(20분) = 32dp
private val AXIS_WIDTH = 52.dp
private val BLOCK_GAP = 3.dp            // 블록 위·아래 여백 → 블록 사이 6dp
private val MIN_CARD_HEIGHT = 26.dp     // 한 줄 글자가 들어가는 최소 높이 (20분 블록 = 32dp - 여백 6dp)

/**
 * 하루 타임라인. 왼쪽 시간축(1시간 글자 + 30분 작은 눈금) 위에 블록을 시간 비율대로 그린다.
 *  - 블록 누르기 → onBlockClick (수정 모달)
 *  - 길게 누른 채 위아래로 끌기 → onBlockMoved (10분 단위, 잠긴 블록은 안 움직임)
 *  - 파란 선 + 오른쪽 끝 화살표: 체크인(→)·체크아웃(←) / 빨간 선: 현재 시각
 */
@Composable
internal fun DayTimelineGrid(
    dayBlocks: List<Block>,
    markers: List<TimeMarker>,
    now: LocalTime?,
    onBlockClick: (Block) -> Unit,
    onBlockMoved: (block: Block, requestedStartMinutes: Int, window: IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val window = remember(dayBlocks, markers) { dayWindowHours(dayBlocks, markers.map { it.time.toMinutes() }) }
    val hours = window.last - window.first
    val windowStartMin = window.first * 60
    val pxPerMinute = with(LocalDensity.current) { HOUR_HEIGHT.toPx() } / 60f
    val totalHeight = HOUR_HEIGHT * hours
    fun yOf(minutes: Int): Dp = HOUR_HEIGHT * ((minutes - windowStartMin) / 60f)

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDy by remember { mutableFloatStateOf(0f) }

    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(totalHeight + 120.dp) // 아래 여백: + 버튼에 마지막 블록이 가리지 않게
                .padding(top = 16.dp),
        ) {
            // 눈금선
            Canvas(Modifier.fillMaxWidth().height(totalHeight)) {
                val axisX = AXIS_WIDTH.toPx()
                val hourPx = HOUR_HEIGHT.toPx()
                val w = 1.dp.toPx()
                for (i in 0..hours) {
                    val y = i * hourPx
                    drawLine(TimelineColors.Axis, Offset(axisX - 8.dp.toPx(), y), Offset(axisX, y), w)  // 1시간 눈금
                    drawLine(TimelineColors.GridLine, Offset(axisX, y), Offset(size.width, y), w)
                    if (i < hours) {
                        val hy = y + hourPx / 2
                        drawLine(TimelineColors.Axis, Offset(axisX - 4.dp.toPx(), hy), Offset(axisX, hy), w) // 30분 눈금
                    }
                }
                drawLine(TimelineColors.Axis, Offset(axisX, 0f), Offset(axisX, size.height), w)
            }

            // 시각 글자 (1시간마다)
            for (h in window) {
                Text(
                    String.format("%02d:00", h),
                    fontSize = 11.sp,
                    color = TimelineColors.SubText,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .offset(y = HOUR_HEIGHT * (h - window.first) - 8.dp)
                        .width(AXIS_WIDTH - 12.dp),
                )
            }

            if (dayBlocks.isEmpty()) {
                Text(
                    "아직 블록이 없어요. 오른쪽 아래 + 버튼으로 추가해 보세요.",
                    fontSize = 13.sp, color = TimelineColors.SubText,
                    modifier = Modifier.offset(y = HOUR_HEIGHT).padding(start = AXIS_WIDTH + 12.dp, end = 16.dp),
                )
            }

            // 블록
            dayBlocks.forEach { block ->
                key(block.id) {
                    val isDragging = draggingId == block.id
                    val top = yOf(block.start.toMinutes()) + BLOCK_GAP
                    val height = (HOUR_HEIGHT * (block.durationMinutes / 60f) - BLOCK_GAP * 2)
                        .coerceAtLeast(MIN_CARD_HEIGHT)
                    val previewStart = if (isDragging) {
                        clampStart(
                            snapMinutes(block.start.toMinutes() + (dragDy / pxPerMinute).roundToInt()),
                            block.durationMinutes, window,
                        )
                    } else null
                    val currentOnMoved by rememberUpdatedState(onBlockMoved)

                    BlockCard(
                        block = block,
                        previewStart = previewStart,
                        modifier = Modifier
                            .zIndex(if (isDragging) 3f else 0f)
                            .padding(start = AXIS_WIDTH + 8.dp, end = 12.dp)
                            .offset { IntOffset(0, (top.toPx() + if (isDragging) dragDy else 0f).roundToInt()) }
                            .fillMaxWidth()
                            .height(height)
                            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(10.dp))
                            .then(
                                if (block.locked) Modifier
                                else Modifier.pointerInput(block, window) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingId = block.id; dragDy = 0f },
                                        onDrag = { change, amount -> change.consume(); dragDy += amount.y },
                                        onDragEnd = {
                                            val requested = block.start.toMinutes() + (dragDy / pxPerMinute).roundToInt()
                                            draggingId = null; dragDy = 0f
                                            currentOnMoved(block, requested, window)
                                        },
                                        onDragCancel = { draggingId = null; dragDy = 0f },
                                    )
                                },
                            )
                            .clickable { onBlockClick(block) },
                    )
                }
            }

            // 체크인(→)·체크아웃(←): 파란 선 + 오른쪽 끝 화살표
            markers.forEach { m ->
                MarkerLine(
                    type = m.type,
                    modifier = Modifier
                        .zIndex(1f)
                        .offset(y = yOf(m.time.toMinutes()) - 6.dp)
                        .padding(start = AXIS_WIDTH, end = 6.dp)
                        .fillMaxWidth(),
                )
            }

            // 현재 시각 빨간 선
            if (now != null) {
                val nowMin = now.toMinutes()
                if (nowMin in windowStartMin..window.last * 60) {
                    Row(
                        Modifier
                            .zIndex(2f)
                            .offset(y = yOf(nowMin) - 4.dp)
                            .padding(start = AXIS_WIDTH - 4.dp)
                            .fillMaxWidth()
                            .height(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(TimelineColors.NowLine))
                        Box(Modifier.weight(1f).height(1.5.dp).background(TimelineColors.NowLine))
                    }
                }
            }
        }
    }
}

/**
 * 파란 선 + 오른쪽 끝 화살표 (글자 없음).
 * 체크인 → 오른쪽 화살표, 체크아웃 → 왼쪽 화살표. 높이 12dp 의 가운데가 정확한 시각 위치.
 */
@Composable
private fun MarkerLine(type: MarkerType, modifier: Modifier = Modifier) {
    Row(modifier.height(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.5.dp).background(TimelineColors.Marker))
        Spacer(Modifier.width(2.dp))
        ArrowIcon(pointRight = type == MarkerType.CHECK_IN, color = TimelineColors.Marker)
    }
}

/** 화살표 아이콘 — 몸통(선) + 삼각형 머리. 라이브러리 없이 직접 그림 */
@Composable
private fun ArrowIcon(pointRight: Boolean, color: Color, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cy = h / 2
        // 오른쪽 기준 좌표를 만들고, 왼쪽이면 좌우 반전
        fun x(r: Float) = if (pointRight) w * r else w * (1f - r)
        drawLine(color, Offset(x(0.05f), cy), Offset(x(0.6f), cy), 1.5.dp.toPx())
        val head = Path().apply {
            moveTo(x(0.5f), h * 0.15f)
            lineTo(x(0.98f), cy)
            lineTo(x(0.5f), h * 0.85f)
            close()
        }
        drawPath(head, color)
    }
}

/* ───────────────────────── 블록 카드 ───────────────────────── */

/**
 * 카드 안쪽 높이에 따라 3가지 배치
 *  - 40dp 미만 (약 30분 이하): 한 줄 — 제목 + 시간
 *  - 40~66dp (약 40분)      : 시간·라벨 / 제목 + 태그
 *  - 66dp 이상 (약 50분 이상): 시간·라벨 / 제목 / 장소·할 일 + 오른쪽 아래 태그
 */
@Composable
private fun BlockCard(block: Block, previewStart: Int?, modifier: Modifier = Modifier) {
    val style = block.cardStyle()
    val shape = RoundedCornerShape(10.dp)
    val timeLabel = previewStart?.let {
        "${minutesToTime(it).format(HM)} ~ ${minutesToTime(it + block.durationMinutes).format(HM)}"
    } ?: block.timeText()
    val label = style.label
    val tags = if (block.kind == BlockKind.FIXED) block.tags else emptyList()

    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(style.bg)
            .blockBorder(style.border, style.dashed)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        val sub = style.fg.copy(alpha = 0.7f)
        when {
            maxHeight < 40.dp -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    block.displayTitle, color = style.fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(timeLabel, color = sub, fontSize = 10.sp, maxLines = 1)
                if (block.locked) {
                    Spacer(Modifier.width(4.dp))
                    LockIcon(sub, 10.dp)
                }
            }

            maxHeight < 66.dp -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                CardTopRow(timeLabel, label, sub, block.locked)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        block.displayTitle, color = style.fg, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    TagPills(tags)
                }
            }

            else -> Box(Modifier.fillMaxSize()) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    CardTopRow(timeLabel, label, sub, block.locked)
                    Text(
                        block.displayTitle, color = style.fg, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (block.kind == BlockKind.FIXED) {
                        val (done, total) = todoProgress(block.todo)
                        val extra = listOfNotNull(
                            block.place?.takeIf { it.isNotBlank() },
                            if (total > 0) "할 일 $done/$total" else null,
                        ).joinToString(" · ")
                        if (extra.isNotEmpty()) {
                            Text(extra, color = sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                TagPills(tags, Modifier.align(Alignment.BottomEnd).padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun CardTopRow(timeLabel: String, label: String, color: Color, locked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(timeLabel, color = color, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(label, color = color, fontSize = 10.sp, maxLines = 1)
        if (locked) {
            Spacer(Modifier.width(3.dp))
            LockIcon(color, 10.dp)
        }
    }
}

/**
 * 자물쇠 아이콘 — 아이콘 라이브러리 없이 직접 그림 (고리 + 몸통).
 * 나중에 디자인 시스템 아이콘이 생기면 이 함수만 바꾸면 된다.
 */
@Composable
internal fun LockIcon(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.14f
        // 고리: 위쪽 반원 + 양옆 세로선
        drawArc(
            color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.05f), size = Size(w * 0.5f, h * 0.5f),
            style = Stroke(width = stroke),
        )
        drawLine(color, Offset(w * 0.25f, h * 0.3f), Offset(w * 0.25f, h * 0.48f), stroke)
        drawLine(color, Offset(w * 0.75f, h * 0.3f), Offset(w * 0.75f, h * 0.48f), stroke)
        // 몸통
        drawRoundRect(
            color = color, topLeft = Offset(w * 0.12f, h * 0.45f), size = Size(w * 0.76f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.12f),
        )
    }
}

/** 태그(#맛집 #명소 #카페) — 흰 배경 타원 */
@Composable
private fun TagPills(tags: List<String>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val pill = RoundedCornerShape(50)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.forEach { tag ->
            Text(
                "#$tag",
                fontSize = 10.sp,
                color = Color(0xFF333333),
                maxLines = 1,
                modifier = Modifier
                    .clip(pill)
                    .background(Color.White)
                    .border(0.5.dp, Color(0x33000000), pill)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}
