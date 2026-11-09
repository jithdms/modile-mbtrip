package com.example.mbtrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LandingScreens(
    onSkipClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // 테마 컬러 정의 (다크 네이비 / 포인트 핑크/블루)
    val PrimaryNavy = Color(0xFF132238)
    val SubTextColor = Color(0xFF666666)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // 1. 상단 건너뛰기 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "건너뛰기",
                fontSize = 14.sp,
                color = SubTextColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onSkipClick() }
            )
        }

        // 앱 타이틀 및 서브 타이틀
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MBTrip",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "나만의 성향으로 떠나는 맞춤 여행",
                fontSize = 14.sp,
                color = SubTextColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 진단 섹션
        LandingSectionTitle(title = "진단")
        Spacer(modifier = Modifier.height(12.dp))

        LandingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "여행 계획 성향 진단", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = "나의 성향은...", fontSize = 11.sp, color = SubTextColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "\"강한 J 성향\"", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "J 100% ‥ P 0%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SubTextColor)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LandingDescription(text = "여행할 때 나는 어떤 성향일까요? 10가지 문항을 통해 나의 J/P 성향을 입체적으로 진단하고 결과를 분석합니다.")

        Spacer(modifier = Modifier.height(36.dp))

        // 3. 방 만들기 섹션
        LandingSectionTitle(title = "방 만들기")
        Spacer(modifier = Modifier.height(12.dp))

        LandingCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(vertical = 12.dp, horizontal = 14.dp)
                ) {
                    Text(text = "학교", fontSize = 13.sp, color = SubTextColor)
                }

                Text(
                    text = "~",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontSize = 16.sp,
                    color = SubTextColor
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(vertical = 12.dp, horizontal = 14.dp)
                ) {
                    Text(text = "집", fontSize = 13.sp, color = SubTextColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LandingDescription(text = "출발지와 도착지, 그리고 여행 기간과 인원수를 설정하여 나만의 여행 방을 손쉽게 개설하고 친구들을 초대하세요.")

        Spacer(modifier = Modifier.height(36.dp))

        // 4. 일정표 섹션
        LandingSectionTitle(title = "일정표")
        Spacer(modifier = Modifier.height(12.dp))

        LandingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "1일차 10/1", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                    Text(text = "확정 67% · 자유 33%", fontSize = 12.sp, color = SubTextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 프로그레스 바 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.67f)
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 내부 타임라인 박스
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = "08:30 ~ 09:40", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "기차로 이동", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LandingDescription(text = "확정 일정과 자유 일정의 블록 비율을 한눈에 파악하고, 동행자와 함께 타임라인을 유기적으로 조율할 수 있습니다.")

        Spacer(modifier = Modifier.height(36.dp))

        // 5. 기록 섹션
        LandingSectionTitle(title = "기록")
        Spacer(modifier = Modifier.height(12.dp))

        LandingCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "여행은 어떠셨나요?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "⭐", fontSize = 20.sp)
                    Text(text = "⭐", fontSize = 20.sp)
                    Text(text = "⭐", fontSize = 20.sp)
                    Text(text = "☆", fontSize = 20.sp)
                    Text(text = "☆", fontSize = 20.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LandingDescription(text = "여행은 어떠셨나요? 즐거웠던 기억을 별점과 함께 기록하고 추억을 영원히 간직하세요.")

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// 공통 재사용 컴포넌트: 섹션 타이틀
@Composable
fun LandingSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF132238)
    )
}

// 공통 재사용 컴포넌트: 카드 레이아웃 (그림자 및 라운드 적용)
@Composable
fun LandingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.LightGray
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD))
    ) {
        content()
    }
}

// 공통 재사용 컴포넌트: 하단 설명 글
@Composable
fun LandingDescription(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = Color(0xFF555555),
        lineHeight = 18.sp
    )
}