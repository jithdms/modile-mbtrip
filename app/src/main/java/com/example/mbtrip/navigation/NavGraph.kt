package com.example.mbtrip.navigation

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
import com.example.mbtrip.ui.component.TripExampleScreen
import com.example.mbtrip.ui.screen.LandingScreen
import com.example.mbtrip.ui.screens.LoginScreen
import com.example.mbtrip.diagnosis.DiagnosisScreen
import com.example.mbtrip.diagnosis.MemberRepository
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.friends.FriendListScreen
import com.example.mbtrip.timeline.fake.FakeBlocks
import kotlinx.coroutines.launch

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")
    object Friends : Screen("friends")
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memberRepository = MemberRepository()
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        // 홈(마이페이지)
        composable(Screen.Home.route) {
            TripExampleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { navController.navigate(Screen.CreateTrip.route) }
            )
        }
        // 초대 생성 화면
        composable(Screen.CreateTrip.route) {
            PlaceholderScreen(
                title = "초대 화면",
                onNext = { navController.popBackStack() },
                onDiagnosis = { navController.navigate(Screen.Diagnosis.route) },
                onTimeline = { navController.navigate(Screen.Timeline.route) }
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
            TimelineScreen(onInviteClick = { navController.navigate(Screen.Friends.route) })
        }
        composable(Screen.Friends.route) {
            FriendListScreen(friends = FakeBlocks.members, onBack = { navController.popBackStack() })
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
