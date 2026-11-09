package com.example.mbtrip.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mbtrip.createtrip.TripSetupScreen
import com.example.mbtrip.createtrip.TripSetupStore
import com.example.mbtrip.createtrip.dateRange
import com.example.mbtrip.createtrip.toMarkers
import com.example.mbtrip.diagnosis.DiagnosisScreen
import com.example.mbtrip.diagnosis.MemberRepository
import com.example.mbtrip.friends.FriendListScreen
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.ui.component.TripExampleScreen
import com.example.mbtrip.ui.screen.LandingScreen
import com.example.mbtrip.ui.screens.LoginScreen
import com.example.mbtrip.ui.screens.MyPageScreen
import kotlinx.coroutines.launch

/**
 * true  = 앱을 켜면 "개발용 화면 목록"이 먼저 나옴 (모든 화면을 바로 열어 보기)
 * false = 원래 흐름: 랜딩 → 로그인 → 방 만들기 …  ← 발표·제출 전에 꼭 false 로!
 */
const val SHOW_DEV_MENU = true

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object DevMenu : Screen("dev_menu")
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")             // 방 만들기 (메인)
    object CreateTrip : Screen("create_trip") // 숙소·교통 입력 (지소은)
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")      // 일정표 (지소은)
    object Friends : Screen("friends")
    object MyPage : Screen("mypage")
    object TripReview : Screen("trip_review") // 여행 후기 (XML 미리보기)
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memberRepository = MemberRepository()
    val friends = remember { FakeBlocks.members.toMutableStateList() }
    NavHost(
        navController = navController,
        startDestination = if (SHOW_DEV_MENU) Screen.DevMenu.route else Screen.Landing.route
    ) {
        // 개발용 화면 목록 (SHOW_DEV_MENU = true 일 때 시작 화면)
        composable(Screen.DevMenu.route) {
            DevMenuScreen(onOpen = { route -> navController.navigate(route) })
        }
        // 방 만들기 (메인)
        composable(Screen.Home.route) {
            TripExampleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { navController.navigate(Screen.CreateTrip.route) }
            )
        }
        // 숙소·교통 입력 (지소은) — 방 만들기 [생성하기] 다음 화면
        composable(Screen.CreateTrip.route) {
            TripSetupScreen(
                onBack = { navController.popBackStack() },
                onInviteClick = { navController.navigate(Screen.Friends.route) },
                onSave = { setup ->
                    TripSetupStore.current = setup
                    navController.navigate(Screen.Timeline.route)
                }
            )
        }
        // 랜딩화면
        composable(Screen.Landing.route) {
            LandingScreen(navController = navController)
        }
        // 로그인/회원가입 화면
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        // 진단 완료 시 샘플 여행 Member 문서에 점수를 저장합니다.
        composable(Screen.Diagnosis.route) {
            DiagnosisScreen(
                onContinue = { navController.popBackStack() },
                onResult = { result ->
                    scope.launch {
                        try {
                            memberRepository.saveJpScore(
                                tripId = FakeBlocks.TRIP_ID,
                                jpScore = result.firestoreJpScore,
                            )
                            Toast.makeText(context, "진단 점수를 Firebase에 저장했어요.", Toast.LENGTH_SHORT).show()
                        } catch (error: Exception) {
                            Toast.makeText(
                                context,
                                "Firebase 저장 실패: ${error.localizedMessage ?: "설정을 확인해주세요."}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
            )
        }
        // 일정표 (지소은)
        composable(Screen.Timeline.route) {
            val setup = TripSetupStore.current
            val range = setup?.dateRange()
            TimelineScreen(
                tripStart = range?.first ?: FakeBlocks.TRIP_START,
                tripEnd = range?.second ?: FakeBlocks.TRIP_END,
                markers = setup?.toMarkers() ?: FakeBlocks.markers,
                onInviteClick = { navController.navigate(Screen.Friends.route) },
                // 집 버튼 → 메인(방 만들기). 그 위에 쌓인 화면은 모두 닫는다
                onHomeClick = {
                    TripSetupStore.current = null
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        // 친구 리스트 (신소율)
        composable(Screen.Friends.route) {
            FriendListScreen(
                friends = friends,
                onBack = { navController.popBackStack() },
                onDeleteFriend = { friend ->
                    memberRepository.removeMember(FakeBlocks.TRIP_ID, friend.userId)
                    friends.removeAll { it.userId == friend.userId }
                },
            )
        }
        // 마이페이지
        composable(Screen.MyPage.route) {
            MyPageScreen(
                onBackClick = { navController.popBackStack() },
                onReTestClick = { navController.navigate(Screen.Diagnosis.route) }
            )
        }
        // 여행 후기 (XML 레이아웃 미리보기)
        composable(Screen.TripReview.route) {
            TripReviewXmlPreview()
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    onNext: () -> Unit,
    onDiagnosis: (() -> Unit)? = null,
    onTimeline: (() -> Unit)? = null
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNext) {
                Text("다음 화면으로 이동")
            }
            if (onDiagnosis != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onDiagnosis) {
                    Text("진단 화면 미리보기")
                }
            }
            if (onTimeline != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onTimeline) {
                    Text("일정표 화면 미리보기")
                }
            }
        }
    }
}
