package com.example.mbtrip

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mbtrip.auth.*
import com.example.mbtrip.navigation.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SessionUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private class Fake(initial: AccountSession?) : SessionAccess {
        val state = MutableStateFlow(initial)
        var signsOut = 0
        override fun current() = state.value
        override fun observe() = state
        override fun signOut() { signsOut++; state.value = null }
    }
    private val signedIn = AccountSession("session-test-user", false, "session@example.com")

    @Test fun rememberedLoginOpensHomeAndLogoutClearsNavigationHistory() {
        val access = Fake(signedIn); lateinit var nav: NavHostController
        compose.setContent { MaterialTheme { nav = rememberNavController(); MBTripNavGraph(nav, access) } }
        compose.onNodeWithText("방 만들기").assertIsDisplayed()
        compose.onNodeWithText("로그아웃").performClick()
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(0, access.signsOut) }
        compose.onNodeWithText("로그아웃").performClick()
        compose.onNodeWithText("로그아웃하기").performClick()
        compose.waitUntil(5000) { nav.currentDestination?.route == Screen.Login.route }
        compose.onNodeWithText("방 만들기").assertDoesNotExist()
        compose.onNodeWithText("로그인하기").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, access.signsOut); assertNull(nav.previousBackStackEntry) }
    }
    @Test fun signOutWhileViewingDiagnosisClearsPrivateScreens() {
        val access = Fake(signedIn); lateinit var nav: NavHostController
        compose.setContent { MaterialTheme { nav = rememberNavController(); MBTripNavGraph(nav, access) } }
        compose.onNodeWithText("J-P 진단하기").performClick()
        compose.runOnIdle { assertEquals(Screen.Diagnosis.route, nav.currentDestination?.route); access.state.value = null }
        compose.waitUntil(5000) { nav.currentDestination?.route == Screen.Login.route }
        compose.runOnIdle { assertNull(nav.previousBackStackEntry) }
    }
    @Test fun changingAccountsClosesThePreviousInviteDialog() {
        val access = Fake(signedIn)
        compose.setContent { MaterialTheme { MBTripNavGraph(sessionAccess = access) } }
        compose.onNodeWithText("초대 코드로 참가").performClick()
        compose.onNode(hasSetTextAction() and hasText("초대 코드", substring = false)).performTextInput("old-account-input")
        compose.runOnIdle { access.state.value = AccountSession("other-user", false, "other@example.com") }
        compose.onNodeWithText("other@example.com").assertIsDisplayed()
        compose.onNodeWithText("old-account-input").assertDoesNotExist()
        compose.onNodeWithText("참가하기").assertDoesNotExist()
    }
    @Test fun anonymousAccountMustLinkBeforeLogout() {
        var links = 0; var logouts = 0
        compose.setContent { MaterialTheme { AccountSessionBar(AccountSession("anon", true, "익명 계정"), false,
            onSignOut = { logouts++ }, onLink = { links++ }) } }
        compose.onNodeWithText("로그아웃").assertDoesNotExist()
        compose.onNodeWithText("이메일 계정 연결").performClick()
        compose.runOnIdle { assertEquals(1, links); assertEquals(0, logouts) }
    }
    @Test fun signedOutUserCannotReopenAPrivateRoute() {
        val access = Fake(null); lateinit var nav: NavHostController
        compose.setContent { MaterialTheme { nav = rememberNavController(); MBTripNavGraph(nav, access) } }
        compose.onNodeWithText("시작하기").assertIsDisplayed()
        compose.runOnIdle { nav.navigate(Screen.Diagnosis.route) }
        compose.waitUntil(5000) { nav.currentDestination?.route == Screen.Login.route }
        compose.runOnIdle { assertNull(nav.previousBackStackEntry) }
    }
}
