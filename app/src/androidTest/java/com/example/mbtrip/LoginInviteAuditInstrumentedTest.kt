package com.example.mbtrip

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** Acceptance gates: blank and rejected credentials must never enter Home.
 * Run explicitly with mbtripLoginAudit=true. Never creates accounts or submits real credentials.
 */
@RunWith(AndroidJUnit4::class)
class LoginInviteAuditInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun requireExplicitAudit() {
        assumeTrue("Authentication acceptance audit is opt-in",
            InstrumentationRegistry.getArguments().getString("mbtripLoginAudit") == "true")
    }

    @Test fun blankCredentialsMustNotEnterHome() {
        compose.onNodeWithText("시작하기").performClick()
        val before = FirebaseAuth.getInstance().currentUser?.uid
        compose.onNodeWithText("로그인하기").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("처리 중…").fetchSemanticsNodes().isEmpty() }
        val after = FirebaseAuth.getInstance().currentUser?.uid
        Log.i("MBTripLoginAudit", "blankCredentials identityChanged=${before != after} authenticatedAfter=${after != null}")
        compose.onNodeWithText("방 만들기").assertDoesNotExist()
        compose.onNodeWithText("로그인하기").assertIsDisplayed()
    }

    @Test fun invalidCredentialsMustNotEnterHome() {
        compose.onNodeWithText("시작하기").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("audit-user@example.invalid")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("audit-invalid-password")
        val before = FirebaseAuth.getInstance().currentUser?.uid
        compose.onNodeWithText("로그인하기").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("처리 중…").fetchSemanticsNodes().isEmpty() }
        val after = FirebaseAuth.getInstance().currentUser?.uid
        Log.i("MBTripLoginAudit", "invalidCredentials identityChanged=${before != after} authenticatedAfter=${after != null}")
        compose.onNodeWithText("방 만들기").assertDoesNotExist()
        compose.onNodeWithText("로그인하기").assertIsDisplayed()
    }
}
