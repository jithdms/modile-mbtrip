package com.example.mbtrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.ui.theme.MBTripTheme
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 일정표 색이 흰 배경 기준이라 1주차에는 라이트 모드로 고정
            MBTripTheme(darkTheme = false, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        TimelineScreen(
                            now = LocalTime.of(14, 10) // 빨간 현재시각 선 테스트용
                        )
                    }
                }
            }
        }
    }
}