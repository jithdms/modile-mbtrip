package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.createtrip.ItemH
import com.example.mbtrip.createtrip.Wheel
import java.time.LocalTime

private val HOURS = (0..23).map { "%02d".format(it) }
private val MINUTES = (0 until 60 step SNAP_MINUTES).map { "%02d".format(it) }

/**
 * 일정 모달의 [시간] 칸을 눌렀을 때: 시작 ~ 종료 를 휠로 고른다 (10분 단위).
 * 시작보다 최소 [MIN_BLOCK_MINUTES]분 늦은 종료만 [완료] 할 수 있다.
 */
@Composable
internal fun TimeRangePickerDialog(
    initialStart: LocalTime,
    initialEnd: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (start: LocalTime, end: LocalTime) -> Unit,
) {
    var startHour by remember { mutableIntStateOf(initialStart.hour) }
    var startMin by remember { mutableIntStateOf(initialStart.minute / SNAP_MINUTES) } // 휠 칸 번호
    var endHour by remember { mutableIntStateOf(initialEnd.hour) }
    var endMin by remember { mutableIntStateOf(initialEnd.minute / SNAP_MINUTES) }

    val s = startHour * 60 + startMin * SNAP_MINUTES
    val e = endHour * 60 + endMin * SNAP_MINUTES
    val error = when {
        e <= s -> "종료가 시작보다 늦어야 해요."
        e - s < MIN_BLOCK_MINUTES -> "최소 ${MIN_BLOCK_MINUTES}분 이상이어야 해요."
        else -> null
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("시간", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 18.sp, color = TimelineColors.SubText) }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                Text("시작", fontSize = 13.sp, color = TimelineColors.SubText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(20.dp))
                Text("종료", fontSize = 13.sp, color = TimelineColors.SubText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))

            Box(Modifier.fillMaxWidth().height(ItemH * 3), contentAlignment = Alignment.Center) {
                // 가운데 줄 = 선택값 띠
                Box(
                    Modifier.fillMaxWidth().height(ItemH)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEDEDED)),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Wheel(HOURS, startHour, Modifier.width(52.dp)) { startHour = it }
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Wheel(MINUTES, startMin, Modifier.width(52.dp)) { startMin = it }
                    }
                    Text("~", fontSize = 18.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Wheel(HOURS, endHour, Modifier.width(52.dp)) { endHour = it }
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Wheel(MINUTES, endMin, Modifier.width(52.dp)) { endMin = it }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                error ?: "${minutesToTime(s).format(HM)} ~ ${minutesToTime(e).format(HM)}  (${e - s}분)",
                fontSize = 14.sp,
                color = if (error != null) TimelineColors.Error else Color.Black,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (error == null) Color(0xFF1E1E1E) else Color(0xFFD6D6D6))
                    .clickable(enabled = error == null) { onConfirm(minutesToTime(s), minutesToTime(e)) },
                contentAlignment = Alignment.Center,
            ) { Text("완료", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
