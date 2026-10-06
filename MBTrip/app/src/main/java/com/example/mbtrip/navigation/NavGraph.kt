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

// 화면 라우트(경로) 정의
sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
}

@Composable
fun MBTripNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        // 1. 랜딩 화면
        composable(Screen.Landing.route) {
            PlaceholderScreen(title = "랜딩 화면", onNext = { navController.navigate(Screen.Login.route) })
        }
        // 2. 로그인 화면
        composable(Screen.Login.route) {
            PlaceholderScreen(title = "로그인 화면", onNext = { navController.navigate(Screen.Home.route) })
        }
        // 3. 홈(마이페이지) 화면
        composable(Screen.Home.route) {
            TripExampleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { navController.navigate(Screen.CreateTrip.route) }
            )
        }
        // 4. 여행 생성 화면
        composable(Screen.CreateTrip.route) {
            PlaceholderScreen(title = "여행 생성·초대 화면", onNext = { navController.popBackStack() })
        }


    }
}

@Composable
fun PlaceholderScreen(title: String, onNext: () -> Unit) {
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
        }
    }
}