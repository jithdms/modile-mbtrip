package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.vote.VoteChoice
import com.example.mbtrip.timeline.vote.canSubmitVote

private val AgreeColor = Color(0xFFA9E5A0)     // 와이어프레임 초록
private val DisagreeColor = Color(0xFFE88080)  // 와이어프레임 빨강

/**
 * 투표 모달 (와이어프레임 '투표하기'): [찬성 | 반대] + 의견 + "의견 보내기".
 * 실제 저장은 VoteActions(최재성 담당) 가 하고, 이 화면은 고른 값만 넘긴다.
 *
 * @param sending true 면 보내는 중 — 버튼·입력 잠금
 */
@Composable
internal fun VoteDialog(
    block: Block,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (VoteChoice, String) -> Unit,
) {
    var choice by remember(block.id) { mutableStateOf<VoteChoice?>(null) }
    var comment by remember(block.id) { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!sending) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "투표하기", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                "${block.timeText()} · ${block.displayTitle}",
                fontSize = 13.sp, color = TimelineColors.SubText, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoteButton("찬성", choice == VoteChoice.AGREE, AgreeColor, !sending, Modifier.weight(1f)) {
                    choice = VoteChoice.AGREE
                }
                VoteButton("반대", choice == VoteChoice.DISAGREE, DisagreeColor, !sending, Modifier.weight(1f)) {
                    choice = VoteChoice.DISAGREE
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                enabled = !sending,
                minLines = 4,
                placeholder = {
                    Text(
                        if (choice == VoteChoice.DISAGREE) "반대하는 이유를 꼭 적어 주세요" else "의견을 남겨 주세요 (선택)",
                        fontSize = 14.sp,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (choice == VoteChoice.DISAGREE && comment.isBlank()) {
                Text("반대할 때는 이유가 필요해요.", fontSize = 12.sp, color = TimelineColors.Error)
            }

            Button(
                onClick = { choice?.let { onSubmit(it, comment.trim()) } },
                enabled = canSubmitVote(choice, comment) && !sending,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
            ) { Text(if (sending) "보내는 중…" else "의견 보내기") }
        }
    }
}

@Composable
private fun VoteButton(
    text: String, selected: Boolean, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(52.dp)
            .clip(shape)
            .background(if (selected) color else lerp(color, Color.White, 0.6f))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color.Black.copy(alpha = 0.6f) else TimelineColors.Border,
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
