package com.example.mbtrip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mbtrip.ui.component.MBTripButton
import com.example.mbtrip.ui.component.MBTripTopBar
import com.example.mbtrip.navigation.Screen

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current

    // 로그인 <-> 회원가입 모드 전환 (true: 로그인, false: 회원가입)
    var isLoginMode by remember { mutableStateOf(true) }

    // 입력 상태 변수들
    var email by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    // 가짜 중복 체크용 기존 회원 데이터 (예시용)
    val existingEmails = listOf("test@naver.com", "mbtrip@google.com")
    val existingIds = listOf("admin", "user123")

    Scaffold(
        topBar = {
            MBTripTopBar(
                title = if (isLoginMode) "로그인" else "회원가입",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color(0xFFF9FBFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) "MBTrip과 함께하는 맞춤 여행" else "새로운 계정 만들기",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A5F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // [회원가입 전용] 이메일 입력창
            if (!isLoginMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("이메일") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA9D7F5),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF1E3A5F),
                        cursorColor = Color(0xFF1E3A5F)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 아이디 입력창 (로그인 시에는 이메일/아이디 통합용으로 사용 가능)
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text(if (isLoginMode) "이메일 또는 아이디" else "아이디") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA9D7F5),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF1E3A5F),
                    cursorColor = Color(0xFF1E3A5F)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력창
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isLoginMode) "비밀번호" else "새 비밀번호") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA9D7F5),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF1E3A5F),
                    cursorColor = Color(0xFF1E3A5F)
                )
            )

            // [회원가입 전용] 비밀번호 확인 입력창
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    label = { Text("비밀번호 확인") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA9D7F5),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF1E3A5F),
                        cursorColor = Color(0xFF1E3A5F)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 실행 버튼
            MBTripButton(
                text = if (isLoginMode) "로그인하기" else "가입 완료하기",
                isPrimary = true,
                onClick = {
                    if (isLoginMode) {
                        // 로그인 성공 시 홈 화면으로 이동
                        Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = false }
                        }
                    } else {
                        // 회원가입 유효성 검사 로직
                        if (email.isBlank() || userId.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        } else if (existingEmails.contains(email)) {
                            Toast.makeText(context, "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show()
                        } else if (existingIds.contains(userId)) {
                            Toast.makeText(context, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                        } else if (password != passwordConfirm) {
                            Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            // 가입 성공 토스트 띄우기
                            Toast.makeText(context, "회원가입이 완료되었습니다. 로그인해주세요.", Toast.LENGTH_SHORT).show()
                            // 로그인 모드로 전환 및 입력값 초기화
                            isLoginMode = true
                            password = ""
                            passwordConfirm = ""
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 로그인 <-> 회원가입 모드 전환 버튼
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    // 모드 바뀔 때 입력창 초기화
                    password = ""
                    passwordConfirm = ""
                }
            ) {
                Text(
                    text = if (isLoginMode) "계정이 없으신가요? 회원가입하기" else "이미 계정이 있으신가요? 로그인하기",
                    color = Color(0xFF1E3A5F)
                )
            }
        }
    }
}