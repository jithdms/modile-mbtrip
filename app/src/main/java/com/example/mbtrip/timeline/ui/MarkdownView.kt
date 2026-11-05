package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** '할 일' 마크다운 미리보기. 체크박스를 누르면 onToggle(원문 줄 번호) */
@Composable
internal fun MarkdownView(
    md: String,
    enabled: Boolean,
    onToggle: (lineIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = remember(md) { parseMarkdown(md) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (md.isBlank()) {
            Text("아직 할 일이 없어요. '편집'에서 적어 보세요.", fontSize = 13.sp, color = TimelineColors.SubText)
        }
        lines.forEach { line ->
            when (line) {
                is MdLine.Heading -> Text(
                    boldText(line.text),
                    fontWeight = FontWeight.Bold,
                    fontSize = when (line.level) { 1 -> 18.sp; 2 -> 16.sp; else -> 14.sp },
                    modifier = Modifier.padding(top = 4.dp),
                )
                is MdLine.Check -> {
                    Row(
                        Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckCircle(line.checked, enabled) { onToggle(line.lineIndex) }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            boldText(line.text),
                            fontSize = 14.sp,
                            color = if (line.checked) TimelineColors.SubText else Color.Black,
                            textDecoration = if (line.checked) TextDecoration.LineThrough else TextDecoration.None,
                        )
                    }
                }
                is MdLine.Bullet -> Row {
                    Text("•  ", fontSize = 14.sp)
                    Text(boldText(line.text), fontSize = 14.sp)
                }
                is MdLine.Paragraph -> Text(boldText(line.text), fontSize = 14.sp)
                is MdLine.Blank -> Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * 동그란 체크 — 빈 원 / 꽉 찬 원 + 흰 체크 표시.
 * 일정표 카드와 할 일 미리보기가 같이 쓴다.
 */
@Composable
internal fun CheckCircle(checked: Boolean, enabled: Boolean, size: Dp = 20.dp, onClick: () -> Unit) {
    val fill = Color(0xFF1E1E1E)
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) fill else Color.White)
            .border(1.5.dp, if (checked) fill else TimelineColors.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Canvas(Modifier.size(size * 0.55f)) {
                val w = this.size.width
                val h = this.size.height
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.52f)
                    lineTo(w * 0.40f, h * 0.82f)
                    lineTo(w * 0.94f, h * 0.20f)
                }
                drawPath(p, Color.White, style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private fun boldText(text: String): AnnotatedString = buildAnnotatedString {
    splitBold(text).forEach { (part, bold) ->
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
