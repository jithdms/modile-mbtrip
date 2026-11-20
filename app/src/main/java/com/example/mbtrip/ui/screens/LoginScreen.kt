package com.example.mbtrip.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mbtrip.R
import com.example.mbtrip.navigation.Screen
import com.example.mbtrip.session.Account
import com.example.mbtrip.ui.component.MBTripTopBar
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.mbFieldColors
import com.example.mbtrip.ui.design.shake
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.DangerSoft
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Muted
import kotlinx.coroutines.launch

/**
 * 로그인 / 회원가입 — Firebase 이메일 계정.
 *  - 로그인: 이메일 또는 아이디 + 비밀번호
 *  - 회원가입: 이메일 · 이름 · 아이디 · 비밀번호(+확인) → users/{uid} 문서도 같이 만든다
 * 성공하면 AfterLogin (진단을 안 했으면 진단부터) 으로 간다.
 */
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 로그인 <-> 회원가입 모드 전환 (true: 로그인, false: 회원가입)
    var isLoginMode by remember { mutableStateOf(true) }

    // 입력 상태 변수들
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCount by remember { mutableIntStateOf(0) } // 같은 오류여도 다시 흔들리게

    fun fail(msg: String) {
        error = msg
        errorCount++
    }

    fun submit() {
        if (busy) return
        error = null
        if (!isLoginMode) {
            when {
                email.isBlank() || nickname.isBlank() || userId.isBlank() || password.isBlank() -> return fail("모든 항목을 입력해 주세요.")
                password != passwordConfirm -> return fail("비밀번호가 일치하지 않아요.")
            }
        }
        scope.launch {
            busy = true
            val problem = if (isLoginMode) {
                Account.signIn(context, userId, password)
            } else {
                Account.signUp(context, email, userId, nickname, password)
            }
            busy = false
            if (problem != null) {
                fail(problem)
            } else {
                // 로그인 뒤: 처음이면 J/P 진단 → 방 만들기, 진단을 했으면 바로 방 만들기
                // 로그인·랜딩 화면은 모두 닫는다 (뒤로 가도 다시 안 나오게)
                navController.navigate(Screen.AfterLogin.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    SkyBackground(Modifier.fillMaxSize(), base = Color.White) {
        Scaffold(
            topBar = {
                MBTripTopBar(
                    title = if (isLoginMode) "로그인" else "회원가입",
                    // 앱을 다시 켜서 로그인부터 시작했으면 뒤에 화면이 없다 → 소개(랜딩)로
                    onBackClick = {
                        if (navController.previousBackStackEntry != null) navController.popBackStack()
                        else navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            },
            containerColor = Color.Transparent,
            // 상태바 여백은 MainActivity 가 이미 줬다
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = isLoginMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.enterFromBelow(0),
                    label = "loginTitle",
                ) { login ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (login) "MBTrip과 함께하는 맞춤 여행" else "새로운 계정 만들기",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Ink,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (login) "다시 만나서 반가워요" else "몇 가지만 적으면 바로 떠날 수 있어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                MBCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterFromBelow(1)
                        // 회원가입 칸이 펼쳐질 때 카드가 부드럽게 늘어난다
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // [회원가입 전용] 이메일 · 이름
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("이메일") },
                                leadingIcon = { MBIcon(R.drawable.ic_mb_mail, size = 18.dp, tint = Muted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = mbFieldColors()
                            )
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                label = { Text("이름 (친구에게 보이는 이름)") },
                                leadingIcon = { MBIcon(R.drawable.ic_mb_person, size = 18.dp, tint = Muted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = mbFieldColors()
                            )
                        }
                    }

                    // 아이디 (로그인 시에는 이메일/아이디 통합)
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text(if (isLoginMode) "이메일 또는 아이디" else "아이디 (영어 소문자·숫자·_)") },
                        leadingIcon = {
                            MBIcon(if (isLoginMode) R.drawable.ic_mb_mail else R.drawable.ic_mb_person, size = 18.dp, tint = Muted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (isLoginMode) KeyboardType.Email else KeyboardType.Ascii),
                        colors = mbFieldColors()
                    )

                    // 비밀번호
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isLoginMode) "비밀번호" else "비밀번호 (6자 이상)") },
                        leadingIcon = { MBIcon(R.drawable.ic_mb_lock, size = 18.dp, tint = Muted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = mbFieldColors()
                    )

                    // [회원가입 전용] 비밀번호 확인
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        OutlinedTextField(
                            value = passwordConfirm,
                            onValueChange = { passwordConfirm = it },
                            label = { Text("비밀번호 확인") },
                            leadingIcon = { MBIcon(R.drawable.ic_mb_check, size = 18.dp, tint = Muted) },
                            isError = passwordConfirm.isNotEmpty() && password != passwordConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = mbFieldColors()
                        )
                    }

                    // 오류 안내 (틀리면 흔들린다)
                    AnimatedVisibility(visible = error != null) {
                        Text(
                            error.orEmpty(),
                            fontSize = 13.sp,
                            color = Danger,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shake(if (error != null) errorCount else null)
                                .background(DangerSoft, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    MBButton(
                        text = if (isLoginMode) "로그인하기" else "가입 완료하기",
                        onClick = { submit() },
                        loading = busy,
                        style = MBButtonStyle.Brand,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 로그인 <-> 회원가입 모드 전환 버튼
                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        // 모드 바뀔 때 비밀번호·오류 초기화
                        password = ""
                        passwordConfirm = ""
                        error = null
                    },
                    enabled = !busy,
                    modifier = Modifier.enterFromBelow(2),
                ) {
                    Text(
                        text = if (isLoginMode) "계정이 없으신가요? " else "이미 계정이 있으신가요? ",
                        color = Muted,
                    )
                    Text(
                        text = if (isLoginMode) "회원가입하기" else "로그인하기",
                        color = Aqua700,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
