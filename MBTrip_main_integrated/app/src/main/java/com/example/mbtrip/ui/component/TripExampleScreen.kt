package com.example.mbtrip.ui.component

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
    onNavigateNext: () -> Unit = {}
) {
    // 입력값 상태 관리 (출발, 도착, 날짜, 명수)
    var departure by remember { mutableStateOf("학교") }
    var arrival by remember { mutableStateOf("집") }
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

            // 하단 생성하기 버튼 (우리가 만든 MBTripButton 활용)
            MBTripButton(
                text = "생성하기",
                onClick = onNavigateNext,
                isPrimary = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}