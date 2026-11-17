package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/*
 * 아이콘 라이브러리 없이 Canvas 로 그린 버튼 (잠금 아이콘과 같은 방식).
 * 누르는 영역은 48dp (안드로이드 권장 최소 크기), 그림은 24dp.
 */

/** ◀ 뒤로가기 */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.Black) {
    TapCircle(onClick, "뒤로 가기", modifier) {
        Canvas(Modifier.size(24.dp)) {
            val w = size.width
            val stroke = 2.4.dp.toPx()
            drawLine(color, Offset(w * 0.85f, w / 2), Offset(w * 0.18f, w / 2), stroke, StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.48f, w * 0.2f)
                lineTo(w * 0.18f, w / 2)
                lineTo(w * 0.48f, w * 0.8f)
            }
            drawPath(head, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** ⌂ 메인(방 만들기)으로 */
@Composable
fun HomeButton(onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.Black) {
    TapCircle(onClick, "메인으로 가기", modifier) {
        Canvas(Modifier.size(24.dp)) {
            val w = size.width
            val stroke = 2.2.dp.toPx()
            val house = Path().apply {
                moveTo(w * 0.1f, w * 0.48f)   // 지붕 왼쪽
                lineTo(w * 0.5f, w * 0.12f)   // 꼭대기
                lineTo(w * 0.9f, w * 0.48f)   // 지붕 오른쪽
                moveTo(w * 0.22f, w * 0.38f)  // 벽
                lineTo(w * 0.22f, w * 0.88f)
                lineTo(w * 0.78f, w * 0.88f)
                lineTo(w * 0.78f, w * 0.38f)
                moveTo(w * 0.42f, w * 0.88f)  // 문
                lineTo(w * 0.42f, w * 0.64f)
                lineTo(w * 0.58f, w * 0.64f)
                lineTo(w * 0.58f, w * 0.88f)
            }
            drawPath(house, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun TapCircle(onClick: () -> Unit, label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}
