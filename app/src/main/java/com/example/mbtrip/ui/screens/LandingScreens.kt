package com.example.mbtrip.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mbtrip.navigation.Screen
import com.example.mbtrip.ui.component.MBTripButton


@Composable
fun LandingScreen(navController: NavController) {
    // 배경은 스크린샷과 같이 부드러운 밝은 톤 또는 화이트로 유지하되, 전체 화면을 감싸줍니다.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FBFC) // 아주 연한 스카이 블루/그레이 톤 배경
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 앱 타이틀 또는 로고 영역
            Text(
                text = "MBTrip",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A5F) // 스크린샷의 진한 네이비 텍스트 톤
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "나만의 성향으로 떠나는 맞춤 여행",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 로그인 화면(또는 다음 단계)으로 넘어가는 Primary 버튼
            // 앞서 만든 공통 버튼 컴포넌트(MBPrimaryButton)를 재사용합니다.
            // LandingScreen.kt 내부의 버튼 호출부 수정
            MBTripButton(
                text = "시작하기",
                isPrimary = true, // Primary 버튼으로 지정
                onClick = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
    }
}