package com.example.mbtrip.timeline.ui

import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua50
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua600
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Coral
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Mint
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

private val AgreeColor = Mint     // 와이어프레임 초록
private val DisagreeColor = Coral  // 와이어프레임 빨강

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
                .enterPop(0, from = 0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "투표하기", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink,
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
                shape = RoundedCornerShape(16.dp),
                colors = com.example.mbtrip.ui.design.mbFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (choice == VoteChoice.DISAGREE && comment.isBlank()) {
                Text("반대할 때는 이유가 필요해요.", fontSize = 12.sp, color = TimelineColors.Error)
            }

            com.example.mbtrip.ui.design.MBButton(
                text = if (sending) "보내는 중…" else "의견 보내기",
                onClick = { choice?.let { onSubmit(it, comment.trim()) } },
                enabled = canSubmitVote(choice, comment) || sending,
                loading = sending,
                height = 50.dp,
            )
        }
    }
}

@Composable
private fun VoteButton(
    text: String, selected: Boolean, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg by androidx.compose.animation.animateColorAsState(if (selected) color else lerp(color, Color.White, 0.6f), label = "voteBg")
    Box(
        modifier
            .height(56.dp)
            .bouncyClick(shape, enabled = enabled, scaleDown = 0.93f, elevation = if (selected) 6.dp else 0.dp, shadowColor = color, onClick = onClick)
            .background(bg)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Ink.copy(alpha = 0.6f) else TimelineColors.Border,
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
