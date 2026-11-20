package com.example.mbtrip.createtrip

import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.R
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.design.shake
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.DangerSoft
import com.example.mbtrip.ui.theme.Field
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.MBBrush
import com.example.mbtrip.ui.theme.Muted

private const val CODE_LENGTH = 6
private val Black = Ink
private val Sub = Muted
private val BoxIdle = Line
private val ErrorRed = Danger

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
                .enterPop(0, from = 0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 둥실 떠 있는 열쇠
            Box(
                Modifier
                    .size(52.dp)
                    .background(Aqua100, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { MBIcon(R.drawable.ic_mb_key, size = 26.dp, tint = Aqua700) }
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
                // 틀린 코드면 좌우로 흔들린다
                modifier = Modifier.focusRequester(focus).shake(if (showError) error else null),
                decorationBox = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(CODE_LENGTH) { i ->
                            val ch = code.getOrNull(i)
                            val isCurrent = i == code.length && !joining
                            CodeBox(
                                index = i,
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
            MBButton(
                text = if (joining) "확인 중…" else "참여하기",
                onClick = { onJoin(code) },
                enabled = ready || joining,
                loading = joining,
                style = MBButtonStyle.Brand,
                height = 52.dp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "취소",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (joining) BoxIdle else Sub,
                modifier = Modifier
                    .bouncyClick(RoundedCornerShape(10.dp), enabled = !joining, scaleDown = 0.9f, onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 코드 한 칸 — 글자가 들어오면 톡 커졌다 돌아오고, 지금 칸은 아쿠아로 빛나며 커서가 깜빡인다 */
@Composable
private fun CodeBox(index: Int, char: Char?, active: Boolean, filled: Boolean, hasError: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor by animateColorAsState(
        when {
            hasError -> ErrorRed
            active -> Aqua500
            filled -> Aqua300
            else -> BoxIdle
        },
        label = "codeBorder",
    )
    val bg by animateColorAsState(
        when {
            hasError -> DangerSoft
            active -> Color.White
            filled -> Aqua100
            else -> Field
        },
        label = "codeBg",
    )
    val borderWidth by animateDpAsState(if (active) 2.dp else 1.dp, label = "codeBorderW")
    Box(
        Modifier
            .enterPop(index, delayStepMs = 45, startDelayMs = 150, from = 0.5f)
            .size(width = 40.dp, height = 52.dp)
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            // key 로 감싸서 글자가 바뀔 때마다 다시 톡 튀어나온다
            androidx.compose.runtime.key(char) {
                Text(
                    char.toString(),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    color = if (hasError) ErrorRed else Black,
                    modifier = Modifier.enterPop(0, from = 0.4f),
                )
            }
        } else if (active) {
            // 지금 칸 표시 — 깜빡이는 커서
            val t = rememberInfiniteTransition(label = "caret")
            val a by t.animateFloat(1f, 0f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "caretA")
            Box(Modifier.size(width = 2.dp, height = 22.dp).alpha(a).background(Aqua700))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "초대코드 입력")
@Composable
private fun JoinCodePreview() {
    JoinCodeDialog(joining = false, error = null, onDismiss = {}, onJoin = {})
}