package com.example.mbtrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.ui.component.MBTripButton
import com.example.mbtrip.ui.component.MBTripTopBar
// 정의하신 테마 컬러 팔레트 Import
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Primary
import com.example.mbtrip.ui.theme.Secondary
import com.example.mbtrip.ui.theme.Surface
import com.example.mbtrip.ui.theme.TextPrimary
import com.example.mbtrip.ui.theme.TextSecondary

@Composable
fun MyPageScreen(
    onBackClick: () -> Unit = {},
    onReTestClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Background, // 앱 공통 배경색 (#F7FBFC)
        topBar = {
            MBTripTopBar(
                title = "마이페이지",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 프로필 영역 (원형 프로필 + 이름/아이디 + 수정 버튼)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f)), // 대표 메인 색상 틴트
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐶",
                            fontSize = 32.sp
                        )
                    }

                    Column {
                        Text(
                            text = "누군기",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@jithdms",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = { /* 프로필 수정 */ }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "프로필 수정",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. MBTI "P" 85% 카드 영역
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface), // Surface 화이트
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“P”",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "85%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    // 게이지 바
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "J", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(text = "P", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(TextSecondary.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.15f)
                                    .background(TextPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 재진단하기 버튼 (Secondary 딥블루 활용)
                    Button(
                        onClick = onReTestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Secondary,
                            contentColor = Surface
                        )
                    ) {
                        Text(
                            text = "재진단하기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. 달력 카드 박스
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "달력",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. 하단 홈으로 돌아가기 버튼 (MBTripButton 공통 컴포넌트)
            MBTripButton(
                text = "홈으로 돌아가기",
                onClick = onBackClick,
                isPrimary = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}