package com.example.mbtrip.timeline.ui

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
import androidx.compose.material3.Checkbox
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
            .background(Color(0xFFF7F7F7))
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
                    val handler: ((Boolean) -> Unit)? = if (enabled) { { _ -> onToggle(line.lineIndex) } } else null
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = line.checked, onCheckedChange = handler, modifier = Modifier.size(32.dp))
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

private fun boldText(text: String): AnnotatedString = buildAnnotatedString {
    splitBold(text).forEach { (part, bold) ->
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
