package com.example.mbtrip.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.mbtrip.navigation.Screen
import com.example.mbtrip.ui.screens.LandingScreens

/** Keep the team's onboarding design and route its action to real authentication. */
@Composable
fun LandingScreen(navController: NavController) {
    LandingScreens(onSkipClick = { navController.navigate(Screen.Login.route) })
}
