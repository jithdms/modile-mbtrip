package com.example.mbtrip.createtrip

import androidx.compose.animation.AnimatedVisibility
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

private val Rainbow = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFE000),
        Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFAF52DE),
    ),
)
private val CardBg = Color(0xFFF5F5F5)
private val Line = Color(0xFF9E9E9E)
private val Hint = Color(0xFFAAAAAA)
private val Dark = Color(0xFF1E1E1E)
private val ErrorRed = Color(0xFFD32F2F)

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
    members: List<Member> = FakeBlocks.members,
    tripStart: LocalDate? = null,
    tripEnd: LocalDate? = null,
    initial: TripSetup = TripSetupStore.current
        ?: TripSetup(dayTrip = tripStart != null && tripStart == tripEnd),
    onBack: () -> Unit = {},
    onInviteClick: () -> Unit = {},
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

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // ── 맨 위: 프로필 줄 ──
        MemberRow(members, onInviteClick, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
        HorizontalDivider()

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── 카드 왼쪽 위: ◀ 뒤로가기 (누르는 영역 48dp, 카드 여백만큼 당겨서 모서리에 붙임) ──
                BackButton(onBack, Modifier.offset(x = (-12).dp, y = (-12).dp))

                // ── 숙소 (당일치기면 숨김) ──
                AnimatedVisibility(visible = !setup.dayTrip) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("숙소 체크인/아웃 시간을\n작성해주세요.", fontSize = 16.sp, color = Color.Black)
                        PickField(setup.checkIn, "체크인 시간을 입력하세요.") { picking = Field.CHECK_IN }
                        PickField(setup.checkOut, "체크아웃 시간을 입력하세요.") { picking = Field.CHECK_OUT }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── 교통: 출발(가는 편) | 도착(오는 편) ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("교통(비행기, KTX 등)", fontSize = 15.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    LegTab(LegDirection.OUTBOUND, tab == LegDirection.OUTBOUND) { tab = LegDirection.OUTBOUND }
                    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(18.dp).background(Line))
                    LegTab(LegDirection.INBOUND, tab == LegDirection.INBOUND) { tab = LegDirection.INBOUND }
                }
                Text(
                    if (tab == LegDirection.OUTBOUND) "가는 편" else "오는 편",
                    fontSize = 12.sp, color = Hint,
                )
                PickField(setup.leg(tab).departure, "출발 시간을 입력하세요.") { picking = Field.LEG_DEPARTURE }
                PickField(setup.leg(tab).arrival, "도착 시간을 입력하세요.") { picking = Field.LEG_ARRIVAL }

                Spacer(Modifier.height(8.dp))
                DayTripButton(setup.dayTrip) { setup = setup.copy(dayTrip = !setup.dayTrip) }

                if (triedSave && problem != null) {
                    Text(problem, fontSize = 13.sp, color = ErrorRed, modifier = Modifier.fillMaxWidth())
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Dark)
                        .clickable {
                            triedSave = true
                            if (problem == null) onSave(setup)
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("저장하기", color = Color.White, fontSize = 16.sp) }
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

/** 누르면 선택창이 열리는 칸 (키보드 입력 없음). 비어 있으면 회색 안내 문구 */
@Composable
private fun PickField(value: LocalDateTime?, placeholder: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (value != null) {
            Text(value.displayText(), fontSize = 16.sp, color = Color.Black)
        } else {
            Text(placeholder, fontSize = 15.sp, color = Hint)
        }
    }
}

@Composable
private fun LegTab(dir: LegDirection, selected: Boolean, onClick: () -> Unit) {
    Text(
        dir.label,
        fontSize = 15.sp,
        color = if (selected) Color.Black else Hint,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(4.dp),
    )
}

/** 🌈 당일치기 버튼 — 켜짐: 무지개로 꽉 참 + ✓ / 꺼짐: 무지개 테두리만 */
@Composable
private fun DayTripButton(on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(40.dp)
            .clip(shape)
            .then(if (on) Modifier.background(Rainbow) else Modifier.background(Color.White))
            .border(2.dp, Rainbow, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (on) "✓ 당일치기 여행이에요." else "당일치기 여행이에요.",
            fontSize = 15.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
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
