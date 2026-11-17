package com.example.mbtrip.travel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun InviteJoinDialog(onDismiss: () -> Unit, onJoin: suspend (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = busy) { }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("초대 코드로 참가") }, text = {
        Column {
            OutlinedTextField(code, { code = it }, label = { Text("초대 코드") }, singleLine = true, enabled = !busy)
            Text("여행을 만든 사람에게 받은 1회용 코드를 입력해주세요.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && code.isNotBlank(), onClick = {
            val parsed = runCatching { InviteCode.normalize(code) }
            if (parsed.isFailure) error = parsed.exceptionOrNull()?.message
            else {
                busy = true; error = null
                scope.launch {
                    try { onJoin(parsed.getOrThrow()) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = if (e is IllegalArgumentException) e.message
                        else "참가하지 못했어요. 만료·취소된 코드인지 확인하고 다시 시도해주세요." }
                    finally { busy = false }
                }
            }
        }) { Text(if (busy) "참가 중…" else "참가하기") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("취소") } })
}

@Composable
fun InviteOwnerPanel(onIssue: suspend () -> String, onRevoke: suspend () -> Unit) {
    var code by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    Column {
        Text("동행자 초대", style = MaterialTheme.typography.titleMedium)
        Text("1명 참가 또는 12시간 후 종료돼요. 새 코드를 발급하면 이전 코드는 취소돼요.", style = MaterialTheme.typography.bodySmall)
        code?.let { value ->
            Text(value)
            TextButton(enabled = !busy, onClick = { clipboard.setText(AnnotatedString(value)); message = "코드를 복사했어요." }) { Text("초대 코드 복사") }
        }
        Row {
            TextButton(enabled = !busy, onClick = {
                busy = true; message = null; code = null
                scope.launch {
                    try { code = onIssue() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "발급하지 못했어요. 연결 상태를 확인하고 다시 시도해주세요." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "처리 중…" else "새 초대 코드 발급") }
            TextButton(enabled = !busy, onClick = {
                busy = true; message = null
                scope.launch {
                    try { onRevoke(); code = null; message = "초대 코드를 취소했어요." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "취소하지 못했어요. 다시 시도해주세요." }
                    finally { busy = false }
                }
            }) { Text("초대 취소") }
        }
        message?.let { Text(it) }
    }
}
