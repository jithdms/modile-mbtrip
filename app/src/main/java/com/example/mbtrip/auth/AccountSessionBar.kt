package com.example.mbtrip.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun AccountSessionBar(session: AccountSession?, busy: Boolean, onSignOut: () -> Unit, onLink: () -> Unit) {
    var confirming by remember(session?.uid) { mutableStateOf(false) }
    var error by remember(session?.uid) { mutableStateOf<String?>(null) }
    Column {
        session?.let { Text(it.label, style = MaterialTheme.typography.bodySmall) }
        if (session?.anonymous == true) {
            Text("여행을 다시 찾으려면 이메일 계정을 연결해주세요.")
            TextButton(enabled = !busy, onClick = onLink) { Text("이메일 계정 연결") }
        } else if (session != null) TextButton(enabled = !busy, onClick = { confirming = true; error = null }) { Text("로그아웃") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false }, title = { Text("로그아웃할까요?") },
        text = { Text("저장한 여행과 진단은 계정에 남아 있어요. 같은 계정으로 로그인하면 다시 불러올 수 있어요.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            try { onSignOut(); confirming = false }
            catch (e: Exception) { confirming = false; error = "로그아웃하지 못했어요. 다시 시도해주세요." }
        }) { Text("로그아웃하기") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("취소") } })
}
