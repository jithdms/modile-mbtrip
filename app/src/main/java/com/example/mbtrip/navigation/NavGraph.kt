package com.example.mbtrip.navigation

import com.example.mbtrip.notifications.NotificationDependencies
import androidx.compose.runtime.collectAsState
import com.example.mbtrip.createtrip.JoinCodeDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mbtrip.session.UserProfile
import com.example.mbtrip.createtrip.InviteStepScreen
import androidx.compose.runtime.LaunchedEffect
import com.example.mbtrip.session.TripSession
import com.example.mbtrip.timeline.ui.TimelineFocus
import com.example.mbtrip.notifications.NotificationsScreen
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
const val SHOW_DEV_MENU = false

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object DevMenu : Screen("dev_menu")
    object Landing : Screen("landing")
    object Login : Screen("login")
    object AfterLogin : Screen("after_login")          // 로그인 뒤 갈림길 (진단 했나?)
    object FirstDiagnosis : Screen("first_diagnosis")  // 처음 J/P 진단
    object InviteStep : Screen("invite_step")          // 방 만들기 → 친구 초대
    object Home : Screen("home")             // 방 만들기 (메인)
    object CreateTrip : Screen("create_trip") // 숙소·교통 입력 (지소은)
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")      // 일정표 (지소은)
    object Friends : Screen("friends")
    object MyPage : Screen("mypage")
    object TripReview : Screen("trip_review") // 여행 후기 (XML 미리보기)
    object Notifications : Screen("notifications")   // 알림 모아보기
    object TripLogGallery : Screen("trip_log_gallery") // 여행 모임 로그 — 사진 모아보기 (XML)
    object TripLogDetail : Screen("trip_log_detail")   // 여행 모임 로그 — 후기 상세 (XML)
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memberRepository = MemberRepository()
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
            var joiningOpen by remember { mutableStateOf(false) }
            var joining by remember { mutableStateOf(false) }
            var joinError by remember { mutableStateOf<String?>(null) }
            if (joiningOpen) {
                JoinCodeDialog(
                    joining = joining,
                    error = joinError,
                    onDismiss = { joiningOpen = false; joinError = null },
                    onJoin = { code ->
                        scope.launch {
                            joining = true
                            joinError = runCatching { TripSession.joinByCode(code, UserProfile.jpScore(context)) }
                                .getOrElse { "참여하지 못했어요: ${it.message}" }
                            joining = false
                            if (joinError == null) {
                                joiningOpen = false
                                TripSetupStore.current = null
                                navController.navigate(Screen.Timeline.route) // 이미 만들어진 여행 → 바로 일정표
                            }
                        }
                    },
                )
            }
            val unread by remember { NotificationDependencies.repository.unreadCount() }.collectAsState(initial = 0)
            TripExampleScreen(
                onJoinWithCode = { joinError = null; joiningOpen = true },
                // 하단 바
                onMenuClick = { navController.navigate(Screen.TripLogGallery.route) },   // ☰ 여행 기록(모임 로그)
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onFriendsClick = { navController.navigate(Screen.Friends.route) },
                onMyPageClick = { navController.navigate(Screen.MyPage.route) },
                hasUnreadNotifications = unread > 0,
                onNavigateBack = { navController.popBackStack() },
                // ① 입력값으로 새 여행 준비 (문제가 있으면 안내 문구 → 다음 화면으로 안 감)
                onCreateTrip = { dep, arr, range, n ->
                    TripSetupStore.current = null
                    TripSession.start(dep, arr, range, n).also { problem ->
                        if (problem == null) {
                            // 여행 문서 + 나를 바로 저장 → 친구 초대 화면에서 내가 보인다
                            scope.launch {
                                runCatching { TripSession.saveBasics(UserProfile.jpScore(context)) }
                                    .onFailure {
                                        Toast.makeText(context, "여행을 저장하지 못했어요: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    }
                },
                onNavigateNext = { navController.navigate(Screen.InviteStep.route) }
            )
        }
        // 방 만들기 → 친구 초대 → 숙소·교통
        composable(Screen.InviteStep.route) {
            InviteStepScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.CreateTrip.route) },
            )
        }
        // 로그인 뒤 갈림길: 진단을 한 적 없으면 진단부터
        composable(Screen.AfterLogin.route) {
            LaunchedEffect(Unit) {
                val next = if (UserProfile.hasDiagnosis(context)) Screen.Home.route else Screen.FirstDiagnosis.route
                navController.navigate(next) { popUpTo(Screen.AfterLogin.route) { inclusive = true } }
            }
        }
        // 처음 J/P 진단 → [계속하기] → 방 만들기
        composable(Screen.FirstDiagnosis.route) {
            DiagnosisScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.FirstDiagnosis.route) { inclusive = true }
                    }
                },
                onResult = { result ->
                    scope.launch {
                        runCatching { UserProfile.saveJpScore(context, result.firestoreJpScore) }
                            .onFailure {
                                Toast.makeText(context, "진단 점수를 저장하지 못했어요: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
            )
        }
        // 숙소·교통 입력 (지소은) — 방 만들기 [생성하기] 다음 화면
        composable(Screen.CreateTrip.route) {
            TripSetupScreen(
                tripStart = TripSession.startDate,
                tripEnd = TripSession.endDate,
                onBack = { navController.popBackStack() },
                onInviteClick = { navController.navigate(Screen.Friends.route) },
                onSave = { setup ->
                    TripSetupStore.current = setup
                    // ② Firestore 에 여행·나(멤버) 저장 → 일정표
                    scope.launch {
                        runCatching { TripSession.save(setup) }
                            .onFailure {
                                Toast.makeText(context, "여행을 저장하지 못했어요: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
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
                            UserProfile.saveJpScore(context, result.firestoreJpScore)
                            memberRepository.saveJpScore(
                                tripId = TripSession.currentTripId,
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
                tripId = TripSession.currentTripId,
                tripStart = TripSession.startDate ?: range?.first ?: FakeBlocks.TRIP_START,
                tripEnd = TripSession.endDate ?: range?.second ?: FakeBlocks.TRIP_END,
                markers = setup?.toMarkers() ?: FakeBlocks.markers,
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onInviteClick = { navController.navigate(Screen.Friends.route) },
                // 집 버튼 → 메인(방 만들기). 그 위에 쌓인 화면은 모두 닫는다
                onHomeClick = {
                    TripSetupStore.current = null
                    TripSession.clear()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        // 친구 리스트 (신소율)
        composable(Screen.Friends.route) {
            FriendListScreen(friends = FakeBlocks.members, onBack = { navController.popBackStack() })
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
        // 알림 모아보기 — 여행 관련 알림을 누르면 일정표로
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpen = { n ->
                    if (n.tripId != null) {
                        // 그 일정 블록을 강조해 달라고 부탁해 두고 일정표로
                        n.blockId?.let { TimelineFocus.focus(it) }
                        if (!navController.popBackStack(Screen.Timeline.route, inclusive = false)) {
                            navController.navigate(Screen.Timeline.route)
                        }
                    }
                },
            )
        }
        // 여행 모임 로그: 사진 모아보기 → 사진을 누르면 후기 상세
        composable(Screen.TripLogGallery.route) {
            TripLogGalleryXml(onPhotoClick = { navController.navigate(Screen.TripLogDetail.route) })
        }
        composable(Screen.TripLogDetail.route) {
            TripLogDetailXml()
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