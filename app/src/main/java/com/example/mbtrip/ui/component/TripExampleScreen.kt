package com.example.mbtrip.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TripExampleScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    /** [생성하기] 때 입력값을 넘겨받는 곳. 문제가 있으면 안내 문구를 돌려주고, 그때는 다음 화면으로 안 넘어간다 */
    onCreateTrip: ((departure: String, arrival: String, dateRange: String, headcount: String) -> String?)? = null,
    /** [초대코드로 참여하기] — null 이면 버튼 숨김 */
    onJoinWithCode: (() -> Unit)? = null,
    // 하단 바: ☰ 여행 기록 · 🔔 알림 · 👥 친구 · 👤 마이페이지
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onMyPageClick: () -> Unit = {},
    /** 안 읽은 알림이 있으면 🔔 옆에 빨간 점 */
    hasUnreadNotifications: Boolean = false,
) {
    val context = LocalContext.current
    // 입력값 상태 관리 (출발, 도착, 날짜, 명수)
    var departure by remember { mutableStateOf("서울") }
    var arrival by remember { mutableStateOf("시흥") }
    var dateRange by remember { mutableStateOf("2026.10.01 ~ 2026.10.03") }
    var headcount by remember { mutableStateOf("3 명") }

    Scaffold(
        topBar = {
            MBTripTopBar(
                title = "방 만들기",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            // 하단 네비게이션바 (기획안 하단 바 형태 반영)
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
                    BottomBarIcon("☰", "여행 기록", onMenuClick)
                    BottomBarIcon("🔔", "알림", onNotificationsClick, dot = hasUnreadNotifications)
                    BottomBarIcon("👥", "친구", onFriendsClick)
                    BottomBarIcon("👤", "마이페이지", onMyPageClick)
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
            // 상단 입력 카드 영역 (기획안 시안 반영)
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
                    // 출발 / 도착 Row
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

                        // 화살표 아이콘 영역 (수정됨: CenterHorizontally)
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

                    // 날짜 입력
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "날짜", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dateRange,
                            onValueChange = { dateRange = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 명수 입력
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "명수", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = headcount,
                            onValueChange = { headcount = it },
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 하단 생성하기 버튼 + 초대코드로 참여하기 (한 덩어리로 묶어 맨 아래에)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                MBTripButton(
                    text = "생성하기",
                    onClick = {
                        val problem = onCreateTrip?.invoke(departure, arrival, dateRange, headcount)
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
}
/** 하단 바 아이콘 하나 — 누르는 영역 48dp, 아래 작은 이름 */
@Composable
private fun BottomBarIcon(icon: String, label: String, onClick: () -> Unit, dot: Boolean = false) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            if (dot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            }
        }
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}