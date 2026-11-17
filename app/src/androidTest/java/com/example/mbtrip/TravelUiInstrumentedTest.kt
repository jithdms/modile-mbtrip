package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import com.example.mbtrip.friends.FriendListScreen
import com.example.mbtrip.recommendation.RecommendationDialog
import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TravelUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun memberSearchFiltersTheLiveListAndShowsUndiagnosedState() {
        compose.setContent {
            MaterialTheme {
                FriendListScreen(listOf(Member("a", "민지", "@minji", 0),
                    Member("b", "준호", "@junho", 50, hasDiagnosis = false)), onBack = {})
            }
        }
        saveScreenshot("members.png")
        compose.onNodeWithText("준호").performClick()
        compose.onNodeWithText("아직 진단하지 않았어요.").assertIsDisplayed()
        compose.onNodeWithText("닫기").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("junho")
        compose.onNodeWithText("준호").assertIsDisplayed()
        compose.onNodeWithText("민지").assertDoesNotExist()
    }

    @Test fun memberRemovalWaitsForTheCallbackAndUpdatesFromNewData() {
        var members by mutableStateOf(listOf(Member("a", "민지", "@minji", 0)))
        var removed: String? = null
        compose.setContent {
            MaterialTheme {
                FriendListScreen(members, onBack = {}, onDeleteFriend = {
                    removed = it.userId
                    members = members.filterNot { m -> m.userId == it.userId }
                })
            }
        }
        compose.onNodeWithContentDescription("친구 메뉴").performClick()
        compose.onNodeWithText("동행자 내보내기").performClick()
        compose.onNodeWithText("내보내기", useUnmergedTree = true).performClick()
        compose.onNodeWithText("동행자가 아직 없어요.").assertIsDisplayed()
        compose.runOnIdle { assertEquals("a", removed) }
    }

    @Test fun recommendationRequiresPreviewBeforeApplyingProposedBlocks() {
        var applied: List<Block>? = null
        compose.setContent {
            MaterialTheme {
                RecommendationDialog(emptyList(), listOf(Member("a", "민지", "@minji", 0)), "trip",
                    LocalDate.of(2026,11,17), {}, { applied = it })
            }
        }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText("배분 미리보기").performClick()
        compose.runOnIdle { assertNull(applied) }
        saveScreenshot("recommendation-preview.png")
        compose.onNodeWithText("일정에 반영").performClick()
        compose.runOnIdle {
            val result = requireNotNull(applied)
            assertTrue(result.any { it.source == BlockSource.RECOMMEND && it.status == BlockStatus.PROPOSED })
            assertEquals(result.size, result.map { it.id }.toSet().size)
        }
    }

    @Test fun failedRemovalKeepsTheMemberVisible() {
        compose.setContent {
            MaterialTheme {
                FriendListScreen(listOf(Member("a", "민지", "@minji", 0)), onBack = {},
                    onDeleteFriend = { error("네트워크 연결을 확인해주세요.") })
            }
        }
        compose.onNodeWithContentDescription("친구 메뉴").performClick()
        compose.onNodeWithText("동행자 내보내기").performClick()
        compose.onNodeWithText("내보내기", useUnmergedTree = true).performClick()
        compose.onNodeWithText("네트워크 연결을 확인해주세요.").assertIsDisplayed()
        compose.onNodeWithText("동행자가 아직 없어요.").assertDoesNotExist()
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), name)
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun missingDiagnosisDisablesAutomaticAllocation() {
        compose.setContent {
            MaterialTheme {
                RecommendationDialog(emptyList(), listOf(Member("a", "민지", "@minji", 50, hasDiagnosis = false)), "trip",
                    LocalDate.of(2026,11,17), {}, {})
            }
        }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText("배분 미리보기").assertIsNotEnabled()
    }
}
