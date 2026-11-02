package com.example.mbtrip.navigation

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mbtrip.R

/** 화면 목록 한 줄: 이름 · 담당 · 경로 */
private data class DevEntry(val name: String, val owner: String, val route: String)

private val entries = listOf(
    DevEntry("① 랜딩 (전체 흐름 시작)", "이수진", Screen.Landing.route),
    DevEntry("② 로그인", "이수진", Screen.Login.route),
    DevEntry("③ 방 만들기 (메인)", "이수진", Screen.Home.route),
    DevEntry("④ 숙소·교통 입력", "지소은", Screen.CreateTrip.route),
    DevEntry("⑤ 일정표", "지소은", Screen.Timeline.route),
    DevEntry("친구 리스트", "신소율", Screen.Friends.route),
    DevEntry("J/P 진단", "", Screen.Diagnosis.route),
    DevEntry("마이페이지", "", Screen.MyPage.route),
    DevEntry("여행 후기 (XML)", "", Screen.TripReview.route),
)

/**
 * 개발용 화면 목록 — 모든 화면이 열리는지 한 번에 확인하는 용도.
 * NavGraph.kt 의 SHOW_DEV_MENU = false 로 바꾸면 나타나지 않는다.
 * 각 화면에서 폰의 뒤로가기를 누르면 이 목록으로 돌아온다.
 */
@Composable
fun DevMenuScreen(onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("개발용 화면 목록", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "발표 전에는 NavGraph.kt 의 SHOW_DEV_MENU 를 false 로 바꿔 주세요.",
            fontSize = 13.sp, color = Color(0xFF757575),
        )
        entries.forEach { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F4F4))
                    .clickable { onOpen(e.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (e.owner.isNotEmpty()) Text(e.owner, fontSize = 13.sp, color = Color(0xFF757575))
            }
        }
    }
}

/** res/layout/activity_trip_review.xml 을 Compose 화면 안에 그대로 띄운다 (모양 확인용) */
@Composable
fun TripReviewXmlPreview() {
    AndroidView(
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.activity_trip_review, null, false) },
        modifier = Modifier.fillMaxSize(),
    )
}
