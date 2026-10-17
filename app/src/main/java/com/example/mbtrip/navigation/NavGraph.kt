package com.example.mbtrip.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mbtrip.data.createTrip
import com.example.mbtrip.data.joinTripByInviteCode
import com.example.mbtrip.diagnosis.DiagnosisScreen
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.ui.component.TripExampleScreen
import com.example.mbtrip.ui.screen.LandingScreen
import com.example.mbtrip.ui.screens.LoginScreen
import com.example.mbtrip.ui.screens.MyPageScreen // 👈 진짜 마이페이지 화면 임포트 추가!

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        // 홈
        composable(Screen.Home.route) {
            TripExampleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { navController.navigate(Screen.CreateTrip.route) },
                onNavigateMyPage = { navController.navigate("mypage") }
            )
        }

        // 마이페이지 화면 연결 (진짜 MyPageScreen 호출)
        composable(route = "mypage") {
            MyPageScreen(
                onBackClick = { navController.popBackStack() },
                onReTestClick = {
                    navController.navigate(Screen.Diagnosis.route)
                }
            )
        }

        // 방 생성 및 초대 코드 합류 화면
        composable(Screen.CreateTrip.route) {
            PlaceholderScreen(
                title = "MBTrip 방 관리",
                onNext = { navController.popBackStack() },
                onDiagnosis = { navController.navigate(Screen.Diagnosis.route) },
                onTimeline = { navController.navigate(Screen.Timeline.route) },
                navController = navController
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

        // 진단 화면
        composable(Screen.Diagnosis.route) {
            DiagnosisScreen(onContinue = { navController.popBackStack() })
        }

        // 일정표 화면
        composable(Screen.Timeline.route) {
            TimelineScreen()
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    onNext: () -> Unit,
    onDiagnosis: (() -> Unit)? = null,
    onTimeline: (() -> Unit)? = null,
    navController: NavHostController
) {
    var tripTitle by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(20.dp))

            // 1. 방 만들기 입력 필드 & 버튼
            OutlinedTextField(
                value = tripTitle,
                onValueChange = { tripTitle = it },
                label = { Text("여행 제목 입력 (예: 제주도 여행)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (tripTitle.isNotBlank()) {
                        createTrip(
                            title = tripTitle,
                            startDate = "2026-11-01",
                            endDate = "2026-11-04",
                            onSuccess = { tripId ->
                                navController.navigate(Screen.Timeline.route)
                            },
                            onFailure = { e ->
                                errorMessage = "방 생성 실패: ${e.message}"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("새로운 방 만들기")
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // 2. 초대 코드로 참여하기 입력 필드 & 버튼
            OutlinedTextField(
                value = inviteCodeInput,
                onValueChange = { inviteCodeInput = it },
                label = { Text("초대 코드 6자리 입력") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (inviteCodeInput.isNotBlank()) {
                        joinTripByInviteCode(
                            inviteCode = inviteCodeInput,
                            onSuccess = { tripId ->
                                navController.navigate(Screen.Timeline.route)
                            },
                            onFailure = { e ->
                                errorMessage = "참여 실패: ${e.message}"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("초대 코드로 참여하기")
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

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}