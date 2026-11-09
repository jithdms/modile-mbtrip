package com.example.mbtrip.createtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private const val CODE_LENGTH = 6
private val Black = Color(0xFF1E1E1E)
private val Sub = Color(0xFF757575)
private val BoxIdle = Color(0xFFD6D6D6)
private val ErrorRed = Color(0xFFD32F2F)

/**
 * 방 만들기 화면의 [초대코드로 참여하기].
 *
 *            초대코드로 참여하기
 *      친구에게 받은 6자리 코드를 넣어 주세요
 *      [a][B][3][x][ ][ ]      ← 한 글자씩 순서대로 채워진다 (지금 칸은 검은 테두리)
 *            대소문자를 구분해요
 *      [        참여하기        ]
 *                 취소
 *
 * 영어 대소문자·숫자만 받는다. 붙여넣기도 되고, 지우기는 뒤에서부터.
 * 6자리가 다 차면 키보드의 완료로도 참여할 수 있다.
 */
@Composable
fun JoinCodeDialog(
    joining: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val ready = code.length == CODE_LENGTH && !joining
    // 틀린 코드 안내는 다시 고치기 시작하면 숨긴다
    val showError = error != null && code.length == CODE_LENGTH
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // 열리자마자 첫 칸에 커서 + 키보드
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = { if (!joining) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔑", fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("초대코드로 참여하기", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Black)
            Spacer(Modifier.height(4.dp))
            Text("친구에게 받은 6자리 코드를 넣어 주세요", fontSize = 13.sp, color = Sub, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))

            // 6칸 입력 — 실제 입력칸은 하나, 화면에는 6개의 박스로 그린다
            BasicTextField(
                value = code,
                onValueChange = { v ->
                    if (!joining) code = v.filter { it.isLetterOrDigit() && it.code < 128 }.take(CODE_LENGTH)
                },
                singleLine = true,
                enabled = !joining,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (ready) onJoin(code) }),
                modifier = Modifier.focusRequester(focus),
                decorationBox = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(CODE_LENGTH) { i ->
                            val ch = code.getOrNull(i)
                            val isCurrent = i == code.length && !joining
                            CodeBox(
                                char = ch,
                                active = isCurrent,
                                filled = ch != null,
                                hasError = showError,
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(10.dp))
            Text(
                if (showError) error!! else "대소문자를 구분해요",
                fontSize = 12.sp,
                color = if (showError) ErrorRed else Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (ready) Black else BoxIdle)
                    .clickable(enabled = ready) { onJoin(code) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (joining) "확인 중…" else "참여하기",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "취소",
                fontSize = 14.sp,
                color = if (joining) BoxIdle else Sub,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !joining, onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 코드 한 칸 */
@Composable
private fun CodeBox(char: Char?, active: Boolean, filled: Boolean, hasError: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        hasError -> ErrorRed
        active || filled -> Black
        else -> BoxIdle
    }
    Box(
        Modifier
            .size(width = 38.dp, height = 50.dp)
            .clip(shape)
            .background(if (filled) Color(0xFFF7F7F7) else Color.White)
            .border(if (active) 2.dp else 1.dp, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            Text(char.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Black)
        } else if (active) {
            // 지금 칸 표시 (깜빡이지 않는 작은 밑줄)
            Box(Modifier.size(width = 14.dp, height = 2.dp).background(Black))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "초대코드 입력")
@Composable
private fun JoinCodePreview() {
    JoinCodeDialog(joining = false, error = null, onDismiss = {}, onJoin = {})
}