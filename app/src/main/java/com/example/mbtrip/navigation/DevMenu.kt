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
import com.example.mbtrip.triplog.TripLogDetailScreen
import com.example.mbtrip.triplog.TripLogGalleryScreen
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.theme.Aqua50
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Muted

/** 화면 목록 한 줄: 이름 · 담당 · 경로 */
private data class DevEntry(val name: String, val owner: String, val route: String)

private val entries = listOf(
    DevEntry("① 랜딩 (전체 흐름 시작)", "이수진", Screen.Landing.route),
    DevEntry("② 로그인", "이수진", Screen.Login.route),
    DevEntry("③ 방 만들기 (메인)", "이수진", Screen.Home.route),
    DevEntry("③-1 친구 초대 (방 만들기 다음)", "지소은", Screen.InviteStep.route),
    DevEntry("④ 숙소·교통 입력", "지소은", Screen.CreateTrip.route),
    DevEntry("⑤ 일정표", "지소은", Screen.Timeline.route),
    DevEntry("친구 리스트", "신소율", Screen.Friends.route),
    DevEntry("J/P 진단", "", Screen.Diagnosis.route),
    DevEntry("마이페이지", "", Screen.MyPage.route),
    DevEntry("여행 후기 (XML)", "", Screen.TripReview.route),
    DevEntry("알림 모아보기", "지소은", Screen.Notifications.route),
    DevEntry("여행 모임 로그 · 사진 모아보기", "", Screen.TripLogGallery.route),
    DevEntry("여행 모임 로그 · 후기 상세", "", Screen.TripLogDetail.route),
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
            .background(Aqua50)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("개발용 화면 목록", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "발표 전에는 NavGraph.kt 의 SHOW_DEV_MENU 를 false 로 바꿔 주세요.",
            fontSize = 13.sp, color = Muted,
        )
        entries.forEachIndexed { i, e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .enterFromBelow(i, delayStepMs = 35)
                    .bouncyClick(RoundedCornerShape(16.dp), scaleDown = 0.97f, elevation = 3.dp, shadowColor = Aqua500) { onOpen(e.route) }
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (e.owner.isNotEmpty()) Text(e.owner, fontSize = 13.sp, color = Aqua700)
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

/**
 * 여행 모임 로그 — 사진 모아보기 (res/layout/activity_trip_log_gallery.xml).
 * 데이터·동작은 triplog/TripLogScreens.kt (Firestore trips/{id}/reviews 를 읽음).
 *  - 왼쪽 위 뒤로 가기 → onBack
 *  - 사진을 누르면 onOpenLog → 후기 상세
 */
@Composable
fun TripLogGalleryXml(onBack: () -> Unit, onOpenLog: () -> Unit) {
    TripLogGalleryScreen(onBack = onBack, onOpenLog = onOpenLog)
}

/** 여행 모임 로그 — 후기 상세 (res/layout/activity_trip_log_detail.xml) */
@Composable
fun TripLogDetailXml(onBack: () -> Unit, onEdit: (com.example.mbtrip.triplog.TripLog) -> Unit = {}) {
    TripLogDetailScreen(onBack = onBack, onEdit = onEdit)
}
