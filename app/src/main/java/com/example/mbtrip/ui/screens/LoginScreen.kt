package com.example.mbtrip.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mbtrip.auth.*
import com.example.mbtrip.navigation.Screen
import com.example.mbtrip.ui.component.MBTripButton
import com.example.mbtrip.ui.component.MBTripTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, onAuthenticated: () -> Unit = {}) {
    val service = remember { FirebaseAccountService() }
    AccountScreen(service, onBack = { navController.popBackStack() }, onSuccess = {
        onAuthenticated()
        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    })
}

@Composable
fun AccountScreen(service: AccountService, onBack: () -> Unit, onSuccess: () -> Unit) {
    var login by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingUid by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = busy) { }
    Scaffold(topBar = {
        MBTripTopBar(title = if (login) "로그인" else "회원가입", onBackClick = { if (!busy) onBack() })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (login) "MBTrip과 함께하는 맞춤 여행" else "새로운 계정 만들기", style = MaterialTheme.typography.titleLarge)
            if (pendingUid == null) {
                OutlinedTextField(email, { email = it }, label = { Text("이메일") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
                if (!login) OutlinedTextField(nickname, { nickname = it }, label = { Text("닉네임") },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), enabled = !busy, modifier = Modifier.fillMaxWidth())
                if (!login) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("비밀번호 확인") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
            } else Text("계정 인증이 완료되었어요. 프로필 저장을 마치면 여행을 시작할 수 있어요.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            MBTripButton(text = when {
                busy -> "처리 중…"
                pendingUid != null -> "프로필 저장 다시 시도"
                login -> "로그인하기"
                else -> "가입 완료하기"
            }, isPrimary = true, enabled = !busy, onClick = {
                val validation = if (pendingUid != null) null else AccountInput.validate(email, password,
                    if (login) null else nickname, if (login) null else confirmation)
                if (validation != null) error = validation
                else {
                    busy = true; error = null
                    scope.launch {
                        try {
                            val uid = pendingUid ?: (if (login) service.signIn(email, password)
                                else service.register(email, password)).also { pendingUid = it }
                            password = ""; confirmation = ""
                            service.completeProfile(uid, if (login) null else nickname)
                            onSuccess()
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            error = if (pendingUid != null) "프로필을 저장하지 못했어요. 연결 상태를 확인하고 다시 시도해주세요."
                                else accountError(e)
                        } finally { busy = false }
                    }
                }
            })
            if (pendingUid == null) TextButton(enabled = !busy, onClick = {
                login = !login; password = ""; confirmation = ""; error = null
            }) { Text(if (login) "계정이 없으신가요? 회원가입하기" else "이미 계정이 있으신가요? 로그인하기") }
        }
    }
}
