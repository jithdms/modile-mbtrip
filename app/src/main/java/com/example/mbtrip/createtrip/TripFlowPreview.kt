package com.example.mbtrip.createtrip

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.ui.TimelineScreen

/**
 * soeun 브랜치 확인용: navigation 라이브러리 없이 [숙소·교통] → [일정표] 를 이어 본다.
 * MainActivity 의 setContent 안에서 TimelineScreen() 대신 TripFlowPreview() 를 부르면 된다.
 * (일요일에 main 으로 합칠 때는 NavGraph 가 이 역할을 하므로 이 파일은 안 써도 됨)
 */
@Composable
fun TripFlowPreview() {
    var onTimeline by remember { mutableStateOf(false) }

    if (!onTimeline) {
        TripSetupScreen(
            onBack = {}, // 방 만들기(메인)는 다른 팀원 화면 → main 에서 NavGraph 로 연결됨
            onSave = { setup ->
                TripSetupStore.current = setup
                onTimeline = true
            },
        )
    } else {
        BackHandler { onTimeline = false } // 폰 뒤로가기 → 숙소·교통으로
        val setup = TripSetupStore.current
        val range = setup?.dateRange()
        TimelineScreen(
            tripStart = range?.first ?: FakeBlocks.TRIP_START,
            tripEnd = range?.second ?: FakeBlocks.TRIP_END,
            markers = setup?.toMarkers() ?: FakeBlocks.markers,
            onHomeClick = {
                TripSetupStore.current = null
                onTimeline = false
            },
        )
    }
}
