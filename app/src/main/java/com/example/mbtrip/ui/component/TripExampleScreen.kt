package com.example.mbtrip.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext

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
    // 하단 바: 메뉴(여행 기록) · 알림 · 그룹(친구) · 마이페이지
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
    var selectingMode by remember { mutableStateOf("START") }

    // 달력 팝업 내부의 현재 연/월 상태
    var calendarYear by remember { mutableStateOf(startDate.year) }
    var calendarMonth by remember { mutableStateOf(startDate.monthValue - 1) } // 0~11

    // 명수 ('명' 고정)
    var headcountNumber by remember { mutableStateOf("3") }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "방 만들기",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenuClick) {
                        BarIcon("menu_burger", "☰", "메뉴")
                    }
                    IconButton(onClick = onNotificationsClick) {
                        // 안 읽은 알림이 있으면 오른쪽 위 빨간 점
                        BadgedBox(badge = { if (hasUnreadNotifications) Badge() }) {
                            BarIcon("bell", "🔔", if (hasUnreadNotifications) "알림, 안 읽은 알림 있음" else "알림")
                        }
                    }
                    IconButton(onClick = onFriendsClick) {
                        BarIcon("users_alt", "👥", "그룹")
                    }
                    IconButton(onClick = onMyPageClick) {
                        BarIcon("user", "👤", "마이페이지")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 출발 / 도착
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "출발", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = departure,
                                onValueChange = { departure = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("→", style = MaterialTheme.typography.titleMedium)
                            Text("←", style = MaterialTheme.typography.titleMedium)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "도착", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = arrival,
                                onValueChange = { arrival = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 2. 날짜 선택 영역 (하나의 큰 상자로 감싸기)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "날짜 선택", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 출발일 영역
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectingMode = "START"
                                            calendarYear = startDate.year
                                            calendarMonth = startDate.monthValue - 1
                                            showCalendarDialog = true
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = startDate.format(dateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // 가운데 고정되는 ~ 기호
                                Text(
                                    text = "~",
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )

                                // 도착일 영역
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectingMode = "END"
                                            calendarYear = endDate.year
                                            calendarMonth = endDate.monthValue - 1
                                            showCalendarDialog = true
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = endDate.format(dateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 3. 명수 입력 ('명' 고정)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "명수", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = headcountNumber,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    headcountNumber = newValue
                                }
                            },
                            suffix = {
                                Text(
                                    text = "명",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier.width(140.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 생성하기 + 초대코드로 참여하기 (한 덩어리로 맨 아래에)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    modifier = Modifier.padding(bottom = if (onJoinWithCode != null) 4.dp else 16.dp)
                )
                if (onJoinWithCode != null) {
                    TextButton(
                        onClick = onJoinWithCode,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) { Text("초대코드로 참여하기") }
                }
            }
        }
    }

    // 날짜 선택용 커스텀 캘린더 다이얼로그 (팝업)
    if (showCalendarDialog) {
        val cal = Calendar.getInstance().apply {
            set(calendarYear, calendarMonth, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectingMode == "START") "출발 날짜 선택" else "도착 날짜 선택",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (calendarMonth == 0) {
                                calendarMonth = 11
                                calendarYear -= 1
                            } else {
                                calendarMonth -= 1
                            }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "이전달")
                        }
                        Text(
                            text = "$calendarYear.${String.format("%02d", calendarMonth + 1)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = {
                            if (calendarMonth == 11) {
                                calendarMonth = 0
                                calendarYear += 1
                            } else {
                                calendarMonth += 1
                            }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "다음달")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 요일 헤더
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 날짜 셀 생성
                    val cells = mutableListOf<Int?>()
                    for (i in 1 until firstDayOfWeek) cells.add(null)
                    for (d in 1..maxDays) cells.add(d)

                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (i in 0 until 7) {
                                val day = if (i < week.size) week[i] else null
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(enabled = day != null) {
                                            if (day != null) {
                                                val selectedDate = LocalDate.of(calendarYear, calendarMonth + 1, day)
                                                if (selectingMode == "START") {
                                                    startDate = selectedDate
                                                    if (startDate.isAfter(endDate)) {
                                                        endDate = startDate.plusDays(2)
                                                    }
                                                } else {
                                                    endDate = selectedDate
                                                    if (endDate.isBefore(startDate)) {
                                                        startDate = endDate
                                                    }
                                                }
                                                showCalendarDialog = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day != null) {
                                        Text(
                                            text = "$day",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }
}
/**
 * 하단 바 아이콘. res/drawable 에 그림(name)이 있으면 그 그림, 없으면 이모지(fallback).
 * 그림 파일을 이름으로 찾아서, 파일이 아직 없어도 빌드가 깨지지 않는다.
 */
@Composable
private fun BarIcon(name: String, fallback: String, contentDescription: String) {
    val context = LocalContext.current
    val resId = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    if (resId != 0) {
        Icon(painter = painterResource(id = resId), contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    } else {
        Text(fallback, fontSize = 20.sp)
    }
}