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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import kotlinx.coroutines.launch
import androidx.compose.runtime.produceState
import com.example.mbtrip.session.TestAccess
import com.example.mbtrip.session.Account
import com.example.mbtrip.triplog.ReviewReminders
import com.example.mbtrip.triplog.ReviewWriteScreen
import com.example.mbtrip.friends.FriendRequests
import com.example.mbtrip.session.MyTrips
import com.example.mbtrip.session.MyTrip
import com.example.mbtrip.ui.component.BottomTab
import com.example.mbtrip.ui.screens.TripListScreen

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
    object MyTrips : Screen("my_trips")      // 내 여행 (만든 방 · 초대받은 방 목록)
    object CreateTrip : Screen("create_trip") // 숙소·교통 입력 (지소은)
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")      // 일정표 (지소은)
    object Friends : Screen("friends")
    object MyPage : Screen("mypage")
    object TripReview : Screen("trip_review") // 여행 후기 (XML 미리보기)
    object Notifications : Screen("notifications")   // 알림 모아보기
    object TripLogGallery : Screen("trip_log_gallery") // 여행 모임 로그 — 사진 모아보기 (XML)
    object TripLogDetail : Screen("trip_log_detail")   // 여행 모임 로그 — 후기 상세 (XML)
    object WriteReview : Screen("write_review")        // 후기 쓰기 (여행이 끝나면 오는 알림에서)
}

/** 랜딩(앱 소개)을 이 폰에서 본 적 있는지 */
object AppIntro {
    private const val PREFS = "mbtrip_app"
    private const val KEY = "landing_seen"
    fun seen(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: android.content.Context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
<<<<<<< HEAD
    val memberRepository = MemberRepository()
    // 앱을 켜면 로그인한 사람의 프로필 · 내 여행 목록을 서버에서 받아 둔다
    LaunchedEffect(Unit) {
        runCatching {
            Account.load(context)
            MyTrips.syncFromServer()
            // 오는 날이 지난 여행 → '후기를 남겨 주세요' 알림
            ReviewReminders.check()
        }
    }
    // 나에게 온 친구 신청 (알림 아이콘 빨간 점에 포함)
    val friendRequests by remember(Account.profile?.uid) { FriendRequests.observeIncoming() }
        .collectAsState(initial = emptyList())
    // 시작 화면: 랜딩(앱 소개)은 이 폰에서 처음 켰을 때만.
    // 로그인해 둔 상태면 바로 방 만들기(진단 전이면 진단), 소개를 본 적 있으면 로그인부터.
    val startRoute = remember {
        when {
            SHOW_DEV_MENU -> Screen.DevMenu.route
            Account.isSignedIn -> Screen.AfterLogin.route
            AppIntro.seen(context) -> Screen.Login.route
            else -> Screen.Landing.route
        }
    }
=======
    val memberRepository = MemberRepository()
    val friends = remember { FakeBlocks.members.toMutableStateList() }
>>>>>>> origin/soeun
    NavHost(
        navController = navController,
        startDestination = startRoute,
        // 화면 전환: 살짝 옆으로 밀리며 겹쳐 바뀐다 (짧고 단순하게)
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280, easing = FastOutSlowInEasing)) { it / 8 } +
                fadeIn(tween(220))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260, easing = FastOutSlowInEasing)) { it / 8 } +
                fadeOut(tween(200))
        },
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
                onMyTripsClick = { navController.navigate(Screen.MyTrips.route) { launchSingleTop = true } },
                onMenuClick = { navController.navigate(Screen.TripLogGallery.route) },   // 여행 기록(모임 로그)
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onFriendsClick = { navController.navigate(Screen.Friends.route) },
                onMyPageClick = { navController.navigate(Screen.MyPage.route) },
                hasUnreadNotifications = unread > 0 || friendRequests.isNotEmpty(),
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
                                        Toast.makeText(context, "인터넷 연결을 확인해 주세요. 여행 정보가 아직 올라가지 않았어요.", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    }
                },
                onNavigateNext = { navController.navigate(Screen.InviteStep.route) }
            )
        }
        // 내 여행 — 만든 방 목록. 누르면 그 여행 일정표로
        composable(Screen.MyTrips.route) {
            val unread by remember { NotificationDependencies.repository.unreadCount() }.collectAsState(initial = 0)
            TripListScreen(
                onBack = { navController.popBackStack() },
                onOpenTrip = { trip ->
                    TripSetupStore.current = null
                    TripSession.open(trip)
                    navController.navigate(Screen.Timeline.route)
                },
                onCreateTrip = {
                    if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                        navController.navigate(Screen.Home.route)
                    }
                },
                onTab = { tab ->
                    when (tab) {
                        BottomTab.MyTrips -> Unit
                        BottomTab.TripLog -> navController.navigate(Screen.TripLogGallery.route)
                        BottomTab.Notifications -> navController.navigate(Screen.Notifications.route)
                        BottomTab.Friends -> navController.navigate(Screen.Friends.route)
                        BottomTab.MyPage -> navController.navigate(Screen.MyPage.route)
                    }
                },
                hasUnreadNotifications = unread > 0 || friendRequests.isNotEmpty(),
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
                // 로그인 직후: 내 여행을 받아 오고, 끝난 여행이면 후기 알림 (화면 이동과 따로 돈다)
                scope.launch { runCatching { MyTrips.syncFromServer(); ReviewReminders.check() } }
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
                        // 결과는 조용히 계정에 남긴다 (실패해도 이 폰에는 기억됨)
                        runCatching { UserProfile.saveJpScore(context, result.firestoreJpScore) }
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
                                Toast.makeText(context, "인터넷 연결을 확인해 주세요. 숙소·교통 정보가 아직 올라가지 않았어요.", Toast.LENGTH_LONG).show()
                            }
                    }
                    navController.navigate(Screen.Timeline.route)
                }
            )
        }
        // 랜딩화면
        composable(Screen.Landing.route) {
            LaunchedEffect(Unit) { AppIntro.markSeen(context) } // 다음부터는 랜딩 없이 시작
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
                    // 결과는 조용히 계정과 지금 여행의 동행자 정보에 남긴다
                    scope.launch {
                        runCatching { UserProfile.saveJpScore(context, result.firestoreJpScore) }
                        runCatching {
                            memberRepository.saveJpScore(
                                tripId = TripSession.currentTripId,
                                jpScore = result.firestoreJpScore,
                            )
                        }
                        runCatching { Account.load(context) }
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
                tripStart = TripSession.startDate ?: range?.first ?: TestAccess.defaultStart(),
                tripEnd = TripSession.endDate ?: range?.second ?: TestAccess.defaultEnd(),
                // 체크인·체크아웃 예시 표시는 테스트 계정만
                markers = setup?.toMarkers() ?: if (TestAccess.isTester) FakeBlocks.markers else emptyList(),
                canSeedTestData = TestAccess.isTester,
                // 오는 날이 지나면 아래에 [후기 쓰러 가기]
                onWriteReview = {
                    ReviewReminders.editing = null
                    val id = TripSession.currentTripId
                    ReviewReminders.target = ReviewReminders.tripFor(id) ?: MyTrip(
                        tripId = id,
                        destination = TripSession.destination,
                        departure = TripSession.departure,
                        startDate = TripSession.startDate,
                        endDate = TripSession.endDate,
                    )
                    navController.navigate(Screen.WriteReview.route)
                },
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
        // 친구 리스트 (신소율) — + 로 친구 추가, 친구 모달에서 삭제
        composable(Screen.Friends.route) {
<<<<<<< HEAD
            FriendListScreen(onBack = { navController.popBackStack() })
=======
            FriendListScreen(
                friends = friends,
                onBack = { navController.popBackStack() },
                onDeleteFriend = { friend ->
                    memberRepository.removeMember(FakeBlocks.TRIP_ID, friend.userId)
                    friends.removeAll { it.userId == friend.userId }
                },
            )
>>>>>>> origin/soeun
        }
        // 마이페이지
        composable(Screen.MyPage.route) {
            MyPageScreen(
                onBackClick = { navController.popBackStack() },
                onReTestClick = { navController.navigate(Screen.Diagnosis.route) },
                onOpenMyTrips = { navController.navigate(Screen.MyTrips.route) { launchSingleTop = true } },
                onOpenFriends = { navController.navigate(Screen.Friends.route) },
                onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
                onOpenLogin = { navController.navigate(Screen.Login.route) },
                // 달력에서 고른 날의 여행 로그 → 로그 상세
                onOpenLog = { navController.navigate(Screen.TripLogDetail.route) },
                // 로그가 없는 지난 여행 → 후기 쓰기
                onWriteReview = { trip ->
                    ReviewReminders.editing = null
                    ReviewReminders.target = trip
                    navController.navigate(Screen.WriteReview.route)
                },
                // 로그아웃 · 탈퇴 → 로그인 화면 (랜딩은 처음 한 번만). 뒤로 가도 이전 화면이 안 나오게 모두 닫는다
                onSignedOut = {
                    TripSetupStore.current = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // 여행 후기 (XML 레이아웃 미리보기)
        composable(Screen.TripReview.route) {
            TripReviewXmlPreview()
        }
        // 알림 모아보기 — 여행 관련 알림을 누르면 일정표로
        composable(Screen.Notifications.route) {
            LaunchedEffect(Unit) { runCatching { ReviewReminders.check() } }
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpen = { n ->
                    if (n.type == com.example.mbtrip.notifications.NotificationType.REVIEW_REQUEST && n.tripId != null) {
                        // 후기 알림 → 후기 쓰기
                        ReviewReminders.editing = null
                        ReviewReminders.target = ReviewReminders.tripFor(n.tripId)
                        navController.navigate(Screen.WriteReview.route)
                    } else if (n.tripId != null) {
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
            TripLogGalleryXml(
                onBack = { navController.popBackStack() },
                onOpenLog = { navController.navigate(Screen.TripLogDetail.route) },
            )
        }
        // 후기 쓰기 → 저장하면 여행 로그로
        composable(Screen.WriteReview.route) {
            val editing = remember { ReviewReminders.editing }
            ReviewWriteScreen(
                trip = ReviewReminders.target,
                editing = editing,
                onBack = {
                    ReviewReminders.editing = null
                    navController.popBackStack()
                },
                onSaved = {
                    ReviewReminders.editing = null
                    navController.popBackStack()
                    // 새 후기면 여행 로그로, 수정이면 보던 상세로 (상세는 바뀐 내용으로 다시 그려진다)
                    if (editing == null) navController.navigate(Screen.TripLogGallery.route)
                },
            )
        }
        composable(Screen.TripLogDetail.route) {
            TripLogDetailXml(
                onBack = { navController.popBackStack() },
                // 수정 아이콘 → 후기 쓰기 화면(내용이 채워진 채로) → 저장하면 다시 상세로
                onEdit = { log ->
                    ReviewReminders.editing = log
                    ReviewReminders.target = ReviewReminders.tripOf(log)
                    navController.navigate(Screen.WriteReview.route)
                },
            )
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