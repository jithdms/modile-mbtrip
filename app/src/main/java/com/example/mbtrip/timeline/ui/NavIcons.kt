package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

/**
 * 🔔 알림 — 안 읽은 알림이 있으면 오른쪽 위에 빨간 점.
 */
@Composable
fun BellButton(onClick: () -> Unit, unread: Int = 0, modifier: Modifier = Modifier, color: Color = Color.Black) {
    TapCircle(onClick, if (unread > 0) "알림, 안 읽은 알림 있음" else "알림", modifier) {
        Box {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width
                val stroke = 2.2.dp.toPx()
                val bell = Path().apply {
                    moveTo(w * 0.18f, w * 0.74f)               // 아래 왼쪽
                    lineTo(w * 0.26f, w * 0.62f)
                    lineTo(w * 0.26f, w * 0.42f)
                    cubicTo(w * 0.26f, w * 0.20f, w * 0.74f, w * 0.20f, w * 0.74f, w * 0.42f) // 둥근 머리
                    lineTo(w * 0.74f, w * 0.62f)
                    lineTo(w * 0.82f, w * 0.74f)               // 아래 오른쪽
                    close()
                }
                drawPath(bell, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color, Offset(w * 0.5f, w * 0.12f), Offset(w * 0.5f, w * 0.2f), stroke, StrokeCap.Round) // 꼭지
                drawLine(color, Offset(w * 0.42f, w * 0.86f), Offset(w * 0.58f, w * 0.86f), stroke, StrokeCap.Round) // 추
            }
            // 안 읽은 알림이 있으면 빨간 점만 (개수는 알림 화면 제목 옆에)
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            }
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
