package com.example.mbtrip.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mbtrip.ui.component.TripExampleScreen
import com.example.mbtrip.ui.screen.LandingScreen
import com.example.mbtrip.ui.screens.LoginScreen
import com.example.mbtrip.diagnosis.DiagnosisScreen

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
    object Diagnosis : Screen("diagnosis")
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
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
                onDiagnosis = { navController.navigate(Screen.Diagnosis.route) }
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
        // 진단은 다른 화면에서 Screen.Diagnosis.route로 이동해 열 수 있습니다.
        composable(Screen.Diagnosis.route) {
            DiagnosisScreen(onContinue = { navController.popBackStack() })
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    onNext: () -> Unit,
    onDiagnosis: (() -> Unit)? = null
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
        }
    }
}
