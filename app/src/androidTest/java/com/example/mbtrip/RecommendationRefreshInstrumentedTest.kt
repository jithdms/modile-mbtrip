package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.mbtrip.recommendation.RecommendationDialog
import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecommendationRefreshInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun serverUpdateInvalidatesPreviewBeforeApplying() {
        val date = LocalDate.of(2026,11,18)
        val blocks = mutableStateOf<List<Block>>(emptyList())
        var applied: List<Block>? = null
        compose.setContent { MaterialTheme { RecommendationDialog(blocks.value, listOf(Member("a", "여행자", "@a", 50)), "trip", date, {}, { applied = it }) } }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText("배분 미리보기").performClick()
        compose.onNodeWithText("일정에 반영").assertIsDisplayed()
        val booking = Block("server-booking", "trip", date, LocalTime.of(12,0), LocalTime.of(13,0),
            BlockKind.FIXED, BlockStatus.CONFIRMED, BlockSource.USER, "다른 동행자의 새 예약", locked = true)
        compose.runOnIdle { blocks.value = listOf(booking) }
        compose.onNodeWithText("일정에 반영").assertDoesNotExist()
        compose.onNodeWithText("배분 미리보기").performClick()
        compose.onNodeWithText("일정에 반영").performClick()
        compose.runOnIdle { assertTrue(applied!!.contains(booking)) }
    }
    @Test fun experienceRequiresOperationCheckAndValidReservationTime() {
        compose.setContent { MaterialTheme { RecommendationDialog(emptyList(), listOf(Member("a", "여행자", "@a", 50)), "trip", LocalDate.of(2026,11,18), {}, {}) } }
        compose.onNodeWithText("체험").performClick()
        compose.onNodeWithContentDescription("선택: 웨이브파크 서핑").assertIsNotEnabled()
        compose.onNodeWithContentDescription("운영확인: 웨이브파크 서핑").performScrollTo().performClick()
        compose.onNodeWithContentDescription("선택: 웨이브파크 서핑").performScrollTo().performClick()
        compose.onNodeWithText("배분 미리보기").performClick()
        compose.onNodeWithText("일정에 반영").assertDoesNotExist()
        compose.onNodeWithContentDescription("예약시각: 웨이브파크 서핑").performScrollTo().performTextInput("14:00")
        compose.onNodeWithText("배분 미리보기").performClick()
        compose.onNodeWithText("일정에 반영").assertIsDisplayed()
    }
}
