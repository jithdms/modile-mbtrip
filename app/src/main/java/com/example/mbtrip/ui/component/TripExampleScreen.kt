package com.example.mbtrip.ui.component

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

@Composable
fun TripExampleScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    var departure by remember { mutableStateOf("학교") }
    var arrival by remember { mutableStateOf("집") }

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
                    Text("☰", style = MaterialTheme.typography.titleLarge)
                    Text("🔔", style = MaterialTheme.typography.titleLarge)
                    Text("👥", style = MaterialTheme.typography.titleLarge)
                    Text("👤", style = MaterialTheme.typography.titleLarge)
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

                    // 2. 날짜 선택 영역 (출발일 ~ 도착일, 물결표 가운데 고정)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "날짜 선택", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 출발일 박스
                            OutlinedCard(
                                onClick = {
                                    selectingMode = "START"
                                    calendarYear = startDate.year
                                    calendarMonth = startDate.monthValue - 1
                                    showCalendarDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = startDate.format(dateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            // 물결표 가운데 고정
                            Text(
                                text = "~",
                                modifier = Modifier.padding(horizontal = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            // 도착일 박스
                            OutlinedCard(
                                onClick = {
                                    selectingMode = "END"
                                    calendarYear = endDate.year
                                    calendarMonth = endDate.monthValue - 1
                                    showCalendarDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = endDate.format(dateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
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

            MBTripButton(
                text = "생성하기",
                onClick = onNavigateNext,
                isPrimary = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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