package com.example.mbtrip.createtrip

import com.example.mbtrip.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.shake
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua50
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Coral
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.DangerSoft
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Field as FieldBg
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Lilac
import com.example.mbtrip.ui.theme.MBBrush
import com.example.mbtrip.ui.theme.Mint
import com.example.mbtrip.ui.theme.Muted
import com.example.mbtrip.ui.theme.Sunny
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.ui.BackButton
import com.example.mbtrip.timeline.ui.MemberRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 어느 칸의 날짜·시간을 고르는 중인지 */
private enum class Field { CHECK_IN, CHECK_OUT, LEG_DEPARTURE, LEG_ARRIVAL }

private val CardBg = Color.White
private val Line = com.example.mbtrip.ui.theme.Line
private val Hint = Faint
private val Dark = Ink
private val ErrorRed = Danger

/**
 * 방 만들기 → [생성하기] 다음 화면: 숙소 체크인·체크아웃, 교통(가는 편·오는 편), 당일치기.
 *
 * @param tripStart, tripEnd 방 만들기에서 고른 여행 기간. 아직 넘겨받지 못하면 null (날짜 제한 없음)
 * @param initial  다시 들어왔을 때 이어서 보여줄 값
 * @param onSave   [저장하기] — 검사를 통과한 값만 넘어온다
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetupScreen(
    /** null 이면 동행자 저장소에서 직접 읽는다 (초대·내보내기가 바로 보임) */
    members: List<Member>? = null,
    tripStart: LocalDate? = null,
    tripEnd: LocalDate? = null,
    initial: TripSetup = TripSetupStore.current
        ?: TripSetup(dayTrip = tripStart != null && tripStart == tripEnd),
    onBack: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onRemoveMember: ((Member) -> Unit)? = null,
    onSave: (TripSetup) -> Unit = {},
) {
    var setup by remember { mutableStateOf(initial) }
    var tab by remember { mutableStateOf(LegDirection.OUTBOUND) }
    var picking by remember { mutableStateOf<Field?>(null) }
    var triedSave by remember { mutableStateOf(false) }

    val problem = setup.problem(tripStart, tripEnd)

    fun valueOf(f: Field): LocalDateTime? = when (f) {
        Field.CHECK_IN -> setup.checkIn
        Field.CHECK_OUT -> setup.checkOut
        Field.LEG_DEPARTURE -> setup.leg(tab).departure
        Field.LEG_ARRIVAL -> setup.leg(tab).arrival
    }

    fun write(f: Field, v: LocalDateTime) {
        setup = when (f) {
            Field.CHECK_IN -> setup.copy(checkIn = v)
            Field.CHECK_OUT -> setup.copy(checkOut = v)
            Field.LEG_DEPARTURE -> setup.withLeg(tab, setup.leg(tab).copy(departure = v))
            Field.LEG_ARRIVAL -> setup.withLeg(tab, setup.leg(tab).copy(arrival = v))
        }
    }

    SkyBackground(Modifier.fillMaxSize(), intensity = 0.7f) {
    Column(Modifier.fillMaxSize()) {
        // ── 맨 위: 프로필 줄 ──
        MemberRow(
            members, onInviteClick,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .enterFromBelow(0, distance = 12.dp),
            onRemoveMember = onRemoveMember,
        )
        HorizontalDivider(color = Line)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(8.dp))
            MBCard(
                Modifier
                    .fillMaxWidth()
                    .enterFromBelow(1, distance = 40.dp)
                    .animateContentSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── 카드 왼쪽 위: ◀ 뒤로가기 (누르는 영역 48dp, 카드 여백만큼 당겨서 모서리에 붙임) ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onBack, Modifier.offset(x = (-12).dp, y = (-6).dp), color = Ink)
                    Text(
                        "숙소 · 교통",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        modifier = Modifier.offset(x = (-8).dp, y = (-6).dp),
                    )
                }

                // ── 숙소 (당일치기면 숨김) ──
                AnimatedVisibility(visible = !setup.dayTrip) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("숙소", icon = R.drawable.ic_mb_hotel)
                        Text("숙소 체크인/아웃 시간을\n작성해주세요.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink, lineHeight = 22.sp)
                        PickField(setup.checkIn, "체크인 시간을 입력하세요.", "IN") { picking = Field.CHECK_IN }
                        PickField(setup.checkOut, "체크아웃 시간을 입력하세요.", "OUT") { picking = Field.CHECK_OUT }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── 교통: 출발(가는 편) | 도착(오는 편) ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("교통(비행기, KTX 등)", icon = R.drawable.ic_mb_train, modifier = Modifier.weight(1f))
                    LegSwitch(tab) { tab = it }
                }
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState == LegDirection.INBOUND
                        (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                    },
                    label = "legTab",
                ) { t ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (t == LegDirection.OUTBOUND) "가는 편" else "오는 편",
                            fontSize = 12.sp, color = Aqua700, fontWeight = FontWeight.SemiBold,
                        )
                        PickField(setup.leg(t).departure, "출발 시간을 입력하세요.", "출발") { picking = Field.LEG_DEPARTURE }
                        PickField(setup.leg(t).arrival, "도착 시간을 입력하세요.", "도착") { picking = Field.LEG_ARRIVAL }
                    }
                }

                Spacer(Modifier.height(8.dp))
                DayTripButton(setup.dayTrip) { setup = setup.copy(dayTrip = !setup.dayTrip) }

                AnimatedVisibility(visible = triedSave && problem != null) {
                    Text(
                        problem.orEmpty(),
                        fontSize = 13.sp,
                        color = ErrorRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DangerSoft, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }

                MBButton(
                    text = "완료하고 일정표 보기",
                    onClick = {
                        triedSave = true
                        if (problem == null) onSave(setup)
                    },
                    style = MBButtonStyle.Brand,
                    height = 52.dp,
                    modifier = Modifier.shake(if (triedSave && problem != null) problem else null),
                )
            }
        }
    }
    }

    // ── 날짜·시간 선택 시트 (달력 + 시간 휠) ──
    val field = picking
    if (field != null) key(field, tab) {
        val leg = setup.leg(tab)
        val marked = when (field) {
            Field.CHECK_OUT -> setup.checkIn?.toLocalDate()
            Field.LEG_ARRIVAL -> leg.departure?.toLocalDate()
            else -> null
        }
        val start = valueOf(field)
            ?: LocalDateTime.of(marked ?: defaultDate(field, setup, tripStart), defaultTime(field))
        DateTimePickerSheet(
            title = sheetTitle(field, tab),
            initial = start,
            minDate = tripStart,
            maxDate = tripEnd,
            markedDate = marked,
            onDismiss = { picking = null },
            onConfirm = { v ->
                write(field, v)
                picking = null
            },
        )
    }
}

private fun sheetTitle(f: Field, tab: LegDirection): String {
    val legName = if (tab == LegDirection.OUTBOUND) "가는 편" else "오는 편"
    return when (f) {
        Field.CHECK_IN -> "체크인 날짜·시간"
        Field.CHECK_OUT -> "체크아웃 날짜·시간"
        Field.LEG_DEPARTURE -> "$legName 출발"
        Field.LEG_ARRIVAL -> "$legName 도착"
    }
}

/** 누르면 선택창이 열리는 칸 (키보드 입력 없음). 비어 있으면 회색 안내 문구, 채워지면 아쿠아로 */
@Composable
private fun PickField(value: LocalDateTime?, placeholder: String, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (value != null) Aqua50 else FieldBg, label = "pickBg")
    val border by animateColorAsState(if (value != null) Aqua300 else Line, label = "pickBorder")
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .bouncyClick(shape, scaleDown = 0.97f, onClick = onClick)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoftBadge(tag, color = if (value != null) Aqua300 else Color.White, textColor = if (value != null) Ink else Muted)
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "pickValue",
        ) { v ->
            if (v != null) {
                Text(v.displayText(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            } else {
                Text(placeholder, fontSize = 15.sp, color = Hint)
            }
        }
        Text("›", fontSize = 22.sp, color = Aqua500)
    }
}

/** 가는 편 | 오는 편 — 알약 안에서 흰 동그라미가 미끄러져 옮겨간다 */
@Composable
private fun LegSwitch(tab: LegDirection, onChange: (LegDirection) -> Unit) {
    val itemW = 64.dp
    val x by animateDpAsState(
        if (tab == LegDirection.OUTBOUND) 0.dp else itemW,
        spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "legX",
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Aqua100)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(width = itemW, height = 32.dp)
                .background(Color.White, RoundedCornerShape(50))
                .border(1.dp, com.example.mbtrip.ui.theme.Line, RoundedCornerShape(50)),
        )
        Row {
            LegTab(LegDirection.OUTBOUND, tab == LegDirection.OUTBOUND, itemW) { onChange(LegDirection.OUTBOUND) }
            LegTab(LegDirection.INBOUND, tab == LegDirection.INBOUND, itemW) { onChange(LegDirection.INBOUND) }
        }
    }
}

@Composable
private fun LegTab(dir: LegDirection, selected: Boolean, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) Ink else Muted, label = "legColor")
    Box(
        Modifier
            .size(width = width, height = 32.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dir.label,
            fontSize = 14.sp,
            color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/**
 * 당일치기 버튼 — 다른 칸(하늘색)과 구분되게 산호색.
 * 꺼짐: 연한 산호색 바탕 + 산호색 테두리 · 글자 / 켜짐: 산호색으로 꽉 차고 흰 글자 + 체크
 */
@Composable
private fun DayTripButton(on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val coral = Coral
    val coralDeep = Color(0xFFD9573F)
    val bg by animateColorAsState(if (on) coral else Color(0xFFFFF0EC), label = "dayTripBg")
    val fg by animateColorAsState(if (on) Color.White else coralDeep, label = "dayTripFg")
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .bouncyClick(shape, onClick = onClick)
            .background(bg, shape)
            .border(1.dp, if (on) coral else coral.copy(alpha = 0.7f), shape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MBIcon(if (on) R.drawable.ic_mb_check else R.drawable.ic_mb_clock, size = 18.dp, tint = fg)
        Spacer(Modifier.width(8.dp))
        Text(
            "당일치기 여행이에요",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

/* ───────── 기본값 ───────── */

/** 선택창을 처음 열 때 보여줄 날짜 */
private fun defaultDate(f: Field, s: TripSetup, tripStart: LocalDate?): LocalDate {
    val base = tripStart ?: LocalDate.now()
    return when (f) {
        Field.CHECK_OUT -> s.checkIn?.toLocalDate()?.plusDays(1) ?: base.plusDays(1)
        else -> base
    }
}

/** 체크인 15:00, 체크아웃 11:00 (국내 숙소에서 흔한 시간 — 일반적으로 알려진 내용), 교통 09:00 */
private fun defaultTime(f: Field): LocalTime = when (f) {
    Field.CHECK_IN -> LocalTime.of(15, 0)
    Field.CHECK_OUT -> LocalTime.of(11, 0)
    else -> LocalTime.of(9, 0)
}


@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun TripSetupPreview() {
    TripSetupScreen(
        initial = TripSetup(checkIn = LocalDateTime.of(2026, 10, 1, 10, 10)),
    )
}
