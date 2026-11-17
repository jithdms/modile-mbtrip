package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.mbtrip.travel.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class InviteUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun malformedCodeDoesNotSubmitAndFailureCanRetry() {
        var calls = 0
        compose.setContent { MaterialTheme { InviteJoinDialog({}, { calls++; error("offline") }) } }
        compose.onNode(hasSetTextAction()).performTextInput("wrong")
        compose.onNodeWithText("참가하기").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNode(hasSetTextAction()).performTextReplacement("A".repeat(32))
        compose.onNodeWithText("참가하기").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithText("참가하지 못했어요. 만료·취소된 코드인지 확인하고 다시 시도해주세요.").assertIsDisplayed()
        compose.onNodeWithText("참가하기").performClick()
        compose.runOnIdle { assertEquals(2, calls) }
    }
    @Test fun pendingJoinDisablesDuplicateRequestsAndDismissal() {
        val gate = CompletableDeferred<Unit>(); var calls = 0; var dismissals = 0
        compose.setContent { MaterialTheme { InviteJoinDialog({ dismissals++ }, { calls++; gate.await() }) } }
        compose.onNode(hasSetTextAction()).performTextInput("B".repeat(32))
        compose.onNodeWithText("참가하기").performClick()
        compose.onNodeWithText("참가 중…").assertIsNotEnabled()
        compose.onNodeWithText("취소").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(0, dismissals); gate.complete(Unit) }
        compose.waitForIdle()
    }
    @Test fun ownerSeesCodeOnlyAfterSuccessfulIssueAndCanRevoke() {
        var fail = true; var revokes = 0
        compose.setContent { MaterialTheme { InviteOwnerPanel({ if (fail) error("offline"); "C".repeat(32) }, { revokes++ }) } }
        compose.onNodeWithText("새 초대 코드 발급").performClick()
        compose.onNodeWithText("초대 코드 복사").assertDoesNotExist()
        compose.runOnIdle { fail = false }
        compose.onNodeWithText("새 초대 코드 발급").performClick()
        compose.onNodeWithText("C".repeat(32)).assertIsDisplayed()
        compose.onNodeWithText("초대 취소").performClick()
        compose.onNodeWithText("초대 코드 복사").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, revokes) }
    }
}
