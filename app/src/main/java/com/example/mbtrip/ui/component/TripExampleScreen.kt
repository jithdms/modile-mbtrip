package com.example.mbtrip.ui.component

import androidx.compose.ui.graphics.SolidColor
import com.example.mbtrip.R
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.MBDateRangeSheet
import com.example.mbtrip.ui.design.RegionPickerSheet
import com.example.mbtrip.siheung.PendingRoutine
import com.example.mbtrip.session.TripSession
import com.example.mbtrip.ui.design.MBIcon
import androidx.compose.animation.core.tween
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.design.mbFieldColors
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua200
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Coral
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Field
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.MBBrush
import com.example.mbtrip.ui.theme.Muted

@Composable
fun TripExampleScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    /**
     * [생성하기] 때 입력값을 넘겨받는 곳 (지소은: 새 여행 만들기).
     * 문제가 있으면 안내 문구를 돌려주고, 그때는 다음 화면으로 안 넘어간다.
     * dateRange 는 "2026.10.01 ~ 2026.10.03" 모양, headcount 는 "3명".
     */
    onCreateTrip: ((departure: String, arrival: String, dateRange: String, headcount: String) -> String?)? = null,
    /** [초대코드로 참여하기] — null 이면 버튼 숨김 */
    onJoinWithCode: (() -> Unit)? = null,
    // 하단 바: 내 여행 · 여행 로그 · 알림 · 친구 · 마이페이지
    onMyTripsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onMyPageClick: () -> Unit = {},
    /** 안 읽은 알림이 있으면 알림 아이콘에 빨간 점 */
    hasUnreadNotifications: Boolean = false,
) {
    val context = LocalContext.current
    // 도착 = 여행 목적지로 저장된다 (시흥 여행 앱이라 기본값을 시흥으로)
    var departure by remember { mutableStateOf("서울") }
    var arrival by remember { mutableStateOf("시흥") }

    // 출발일 및 도착일 상태 관리
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    var startDate by remember { mutableStateOf(LocalDate.of(2026, 10, 1)) }
    var endDate by remember { mutableStateOf(LocalDate.of(2026, 10, 3)) }

    // 달력 팝업 표시 여부 및 선택 모드 ("START" = 출발일 선택 중, "END" = 도착일 선택 중)
    var showCalendarDialog by remember { mutableStateOf(false) }
    // 지역 고르기 모달 ("출발" / "도착" / null)
    var pickingRegion by remember { mutableStateOf<String?>(null) }


    // 명수 ('명' 고정)
    var headcountNumber by remember { mutableStateOf("3") }

    // 출발 ⇄ 도착 바꾸기 버튼 (누르면 반 바퀴 돈다)
    var swapTurns by remember { mutableIntStateOf(0) }
    val swapRotation by animateFloatAsState(swapTurns * 180f, tween(280), label = "swap")

    SkyBackground(Modifier.fillMaxSize(), intensity = 0.9f) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
                ) {
                    Text(
                        "어디로 떠나볼까요?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.enterFromBelow(0),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "방 만들기",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink,
                        modifier = Modifier.enterFromBelow(1, distance = 14.dp),
                    )
                }
            },
            bottomBar = {
                MBBottomBar(
                    selected = null,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onSelect = { tab ->
                        when (tab) {
                            BottomTab.MyTrips -> onMyTripsClick()
                            BottomTab.TripLog -> onMenuClick()
                            BottomTab.Notifications -> onNotificationsClick()
                            BottomTab.Friends -> onFriendsClick()
                            BottomTab.MyPage -> onMyPageClick()
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MBCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterFromBelow(2, distance = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 출발 / 도착
                    Column {
                        SectionLabel("어디에서 어디로", icon = R.drawable.ic_mb_place)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RegionTile(
                                caption = "출발",
                                value = departure,
                                active = pickingRegion == "출발",
                                modifier = Modifier.weight(1f),
                            ) { pickingRegion = "출발" }

                            // 가운데 ⇄ — 누르면 출발·도착이 바뀌며 빙글 돈다
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(40.dp)
                                    .bouncyClick(CircleShape) {
                                        val d = departure
                                        departure = arrival
                                        arrival = d
                                        swapTurns++
                                    }
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Line, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                MBIcon(
                                    R.drawable.ic_mb_swap, size = 20.dp, tint = Aqua700,
                                    contentDescription = "출발·도착 바꾸기",
                                    modifier = Modifier.rotate(swapRotation),
                                )
                            }

                            RegionTile(
                                caption = "도착",
                                value = arrival,
                                active = pickingRegion == "도착",
                                modifier = Modifier.weight(1f),
                            ) { pickingRegion = "도착" }
                        }
                    }

                    // 2. 날짜 선택 영역 (하나의 큰 상자로 감싸기)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("날짜 선택", icon = R.drawable.ic_mb_calendar, modifier = Modifier.weight(1f))
                            // 몇 박 며칠 — 날짜가 바뀌면 위아래로 넘어가며 바뀐다
                            val nights = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0)
                            AnimatedContent(
                                targetState = nights,
                                transitionSpec = { numberSlide(targetState > initialState) },
                                label = "nights",
                            ) { n ->
                                SoftBadge(if (n == 0L) "당일치기" else "${n}박 ${n + 1}일")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Field)
                                .border(1.dp, Line, RoundedCornerShape(18.dp))
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 출발일 영역
                            DateTile(
                                caption = "가는 날",
                                text = startDate.format(dateFormatter),
                                active = showCalendarDialog,
                                modifier = Modifier.weight(1f),
                            ) { showCalendarDialog = true }

                            // 가운데 고정되는 ~ 기호
                            Text(
                                text = "~",
                                modifier = Modifier.padding(horizontal = 6.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Faint,
                                textAlign = TextAlign.Center
                            )

                            // 도착일 영역
                            DateTile(
                                caption = "오는 날",
                                text = endDate.format(dateFormatter),
                                active = showCalendarDialog,
                                modifier = Modifier.weight(1f),
                            ) { showCalendarDialog = true }
                        }
                    }

                    // 3. 명수 ('명' 고정) — − / + 로 조절, 숫자는 넘어가며 바뀐다
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("명수", icon = R.drawable.ic_mb_group, modifier = Modifier.weight(1f))
                        val n = headcountNumber.toIntOrNull() ?: 1
                        StepButton("−", enabled = n > 1) { headcountNumber = (n - 1).coerceAtLeast(1).toString() }
                        AnimatedContent(
                            targetState = n,
                            transitionSpec = { numberSlide(targetState > initialState) },
                            modifier = Modifier.width(64.dp),
                            label = "headcount",
                        ) { v ->
                            Text(
                                "${v}명",
                                style = MaterialTheme.typography.titleLarge,
                                color = Ink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        StepButton("+", enabled = n < 30) { headcountNumber = (n + 1).coerceAtMost(30).toString() }
                    }
                }

                // 생성하기 + 초대코드로 참여하기 (한 덩어리로 맨 아래에)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterFromBelow(4, distance = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MBTripButton(
                        text = "생성하기",
                        onClick = {
                            val range = "${startDate.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}"
                            val problem = onCreateTrip?.invoke(departure, arrival, range, "${headcountNumber}명")
                            if (problem != null) {
                                Toast.makeText(context, problem, Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateNext()
                            }
                        },
                        isPrimary = true,
                        modifier = Modifier.padding(bottom = if (onJoinWithCode != null) 10.dp else 16.dp)
                    )
                    if (onJoinWithCode != null) {
                        MBButton(
                            text = "초대코드로 참여하기",
                            onClick = onJoinWithCode,
                            style = MBButtonStyle.Outline,
                            leadingIcon = R.drawable.ic_mb_key,
                            height = 48.dp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                // 추천 투어 루틴 — 미리보기 → [이 순서로 돌아다니기] 로 바로 여행 만들기
                TourRoutineSection(
                    modifier = Modifier.enterFromBelow(5, distance = 40.dp).padding(bottom = 16.dp),
                    onApply = { routine ->
                        // 루틴은 시흥 코스라 도착을 시흥으로, 루틴이 더 길면 오는 날을 늘린다
                        arrival = "시흥"
                        val needEnd = startDate.plusDays((routine.days.size - 1).toLong())
                        val extended = endDate.isBefore(needEnd)
                        if (extended) endDate = needEnd
                        val range = "${startDate.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}"
                        val problem = onCreateTrip?.invoke(departure, "시흥", range, "${headcountNumber}명")
                        if (problem != null) {
                            Toast.makeText(context, problem, Toast.LENGTH_SHORT).show()
                        } else {
                            TripSession.tripId?.let { PendingRoutine.arm(routine, it) }
                            Toast.makeText(
                                context,
                                if (extended) "${routine.lengthLabel} 코스라 오는 날을 ${endDate.format(dateFormatter)}로 맞췄어요."
                                else "일정표를 열면 '${routine.title}' 순서대로 들어가 있어요.",
                                Toast.LENGTH_SHORT,
                            ).show()
                            onNavigateNext()
                        }
                    },
                )
            }
        }
    }

    // 기간 선택 — 앱 공용 달력 모달. 달력 하나에서 가는 날 → 오는 날 순서로 누른다
    if (showCalendarDialog) {
        MBDateRangeSheet(
            title = "여행 날짜",
            initialStart = startDate,
            initialEnd = endDate,
            startLabel = "가는 날",
            endLabel = "오는 날",
            onDismiss = { showCalendarDialog = false },
            onConfirm = { s, e ->
                startDate = s
                endDate = e
                showCalendarDialog = false
            },
        )
    }

    // 출발·도착 지역 목록
    pickingRegion?.let { which ->
        RegionPickerSheet(
            title = "$which 지역",
            selected = if (which == "출발") departure else arrival,
            onDismiss = { pickingRegion = null },
            onPick = { city ->
                if (which == "출발") departure = city else arrival = city
                pickingRegion = null
            },
        )
    }
}

/* ───────────────────────── 작은 부품 ───────────────────────── */

/** 숫자가 커지면 위로, 작아지면 아래로 넘어간다 */
private fun numberSlide(up: Boolean): ContentTransform =
    if (up) {
        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
    } else {
        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
    }

/** 출발 / 도착 칸 — 누르면 지역 목록이 열린다 */
@Composable
private fun RegionTile(caption: String, value: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val border by animateColorAsState(if (active) Aqua500 else Line, label = "regionBorder")
    Column(
        modifier
            .bouncyClick(shape, onClick = onClick)
            .background(Color.White, shape)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(caption, fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value.ifBlank { "선택" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isBlank()) Faint else Ink,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            MBIcon(R.drawable.ic_mb_chevron_right, size = 18.dp, tint = Muted)
        }
    }
}

/** 가는 날 / 오는 날 칸 — 누르면 기간 달력이 열린다 */
@Composable
private fun DateTile(caption: String, text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) Aqua200 else Color.White, label = "dateBg")
    Column(
        modifier
            .bouncyClick(RoundedCornerShape(14.dp), scaleDown = 0.95f, onClick = onClick)
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(caption, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .bouncyClick(CircleShape, enabled = enabled, onClick = onClick)
            .background(if (enabled) Color.White else Field, CircleShape)
            .border(1.dp, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = if (enabled) Ink else Faint)
    }
}
