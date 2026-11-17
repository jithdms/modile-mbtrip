package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.mbtrip.auth.AccountService
import com.example.mbtrip.ui.screens.AccountScreen
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccountScreenInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private class Fake : AccountService {
        var authCalls = 0
        var profileCalls = 0
        var failAuth = false
        var failProfile = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun signIn(email: String, password: String): String {
            authCalls++; gate?.await(); if (failAuth) error("rejected"); return "uid"
        }
        override suspend fun register(email: String, password: String) = signIn(email, password)
        override suspend fun completeProfile(uid: String, nickname: String?) {
            profileCalls++; if (failProfile) error("offline")
        }
    }
    private fun fill() {
        compose.onNode(hasSetTextAction() and hasText("이메일")).performTextInput("test@example.com")
        compose.onNode(hasSetTextAction() and hasText("비밀번호", substring = false)).performTextInput("123456")
    }
    @Test fun blankAndRejectedCredentialsNeverNavigate() {
        val fake = Fake().apply { failAuth = true }; var success = 0
        compose.setContent { MaterialTheme { AccountScreen(fake, {}, { success++ }) } }
        compose.onNodeWithText("로그인하기").performClick()
        compose.runOnIdle { assertEquals(0, fake.authCalls); assertEquals(0, success) }
        fill(); compose.onNodeWithText("로그인하기").performClick()
        compose.runOnIdle { assertEquals(1, fake.authCalls); assertEquals(0, success) }
    }
    @Test fun pendingRequestDisablesDuplicateSubmission() {
        val fake = Fake().apply { gate = CompletableDeferred() }; var success = 0
        compose.setContent { MaterialTheme { AccountScreen(fake, {}, { success++ }) } }
        fill(); compose.onNodeWithText("로그인하기").performClick()
        compose.onNodeWithText("처리 중…").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, fake.authCalls); assertEquals(0, success); fake.gate!!.complete(Unit) }
        compose.waitUntil(5000) { success == 1 }
    }
    @Test fun profileRetryDoesNotCreateOrAuthenticateAgain() {
        val fake = Fake().apply { failProfile = true }; var success = 0
        compose.setContent { MaterialTheme { AccountScreen(fake, {}, { success++ }) } }
        compose.onNodeWithText("계정이 없으신가요? 회원가입하기").performClick()
        fill(); compose.onNode(hasSetTextAction() and hasText("닉네임")).performTextInput("여행자")
        compose.onNode(hasSetTextAction() and hasText("비밀번호 확인")).performTextInput("123456")
        compose.onNodeWithText("가입 완료하기").performClick()
        compose.onNodeWithText("프로필 저장 다시 시도").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, success); fake.failProfile = false }
        compose.onNodeWithText("프로필 저장 다시 시도").performClick()
        compose.runOnIdle { assertEquals(1, success); assertEquals(1, fake.authCalls); assertEquals(2, fake.profileCalls) }
    }
}
