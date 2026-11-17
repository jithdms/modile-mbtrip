package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.mbtrip.ui.component.TripExampleScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MergedHomeInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calendarAndSelectedTripFieldsReachTheRealCreateCallback() {
        val format = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val today = LocalDate.now().format(format)
        val tomorrow = LocalDate.now().plusDays(1).format(format)
        var submitted: List<String>? = null
        compose.setContent { MaterialTheme { TripExampleScreen(onCreateTrip = { a, b, c, d -> submitted = listOf(a,b,c,d) }) } }
        compose.onNodeWithText(today).performScrollTo().performClick()
        compose.onNodeWithText("출발 날짜 선택").assertIsDisplayed()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("생성하기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("학교", "시흥", "$today ~ $tomorrow", "3 명"), submitted) }
    }
}
