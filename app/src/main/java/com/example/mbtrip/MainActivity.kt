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
import com.example.mbtrip.navigation.MBTripNavGraph
import com.example.mbtrip.session.MyTrips
import androidx.compose.ui.graphics.Color
import com.example.mbtrip.ui.theme.MBTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MyTrips.init(applicationContext) // 내 여행 목록 불러오기
        setContent {
            // 일정표 색이 흰 배경 기준이라 라이트 모드로 고정
            MBTripTheme(darkTheme = false, dynamicColor = false) {
                // 상태바·내비게이션 바 뒤도 앱 하늘색으로
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.White) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        // 앱 전체 화면 흐름 (시작 화면은 NavGraph.kt 의 SHOW_DEV_MENU 로 결정)
                        MBTripNavGraph()
                    }
                }
            }
        }
    }
}
