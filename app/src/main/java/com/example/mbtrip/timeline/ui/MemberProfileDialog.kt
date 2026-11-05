package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.timeline.model.Member

private val CardBg = Color(0xFFF4F4F4)
private val CardBlack = Color(0xFF1E1E1E)
private val PColor = Color(0xFFA9E5A0) // P = 자유 블록 초록
private val JColor = Color(0xFFAED6F1) // J = 확정 블록 하늘

/** jpScore(0=P ~ 100=J) → 한 줄 설명 */
internal fun jpLabel(score: Int): String = when {
    score >= 80 -> "확실한 J · 계획형"
    score >= 60 -> "J 쪽 · 계획을 좋아해요"
    score > 40 -> "J·P 반반 · 상황에 맞춰요"
    score > 20 -> "P 쪽 · 즉흥을 좋아해요"
    else -> "확실한 P · 즉흥형"
}

/**
 * 상단 프로필 사진을 눌렀을 때: 사진 · 닉네임 · @아이디 · 한 줄 소개 · J/P 성향.
 */
@Composable
internal fun MemberProfileDialog(
    member: Member,
    onDismiss: () -> Unit,
    /** null 이면 내보내기 버튼을 숨긴다 (예: 혼자 남았을 때) */
    onRemove: (() -> Unit)? = null,
) {
    val score = member.jpScore.coerceIn(0, 100)
    var confirmRemove by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 닫기
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(32.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 16.sp, color = TimelineColors.SubText) }
            }

            MemberAvatar(member, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text(member.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(member.handle, fontSize = 13.sp, color = TimelineColors.SubText)

            // 한 줄 소개 (본인이 적은 글)
            Spacer(Modifier.height(12.dp))
            val bio = member.bio?.trim().orEmpty()
            Text(
                if (bio.isNotEmpty()) "“${bio}”" else "아직 한 줄 소개가 없어요.",
                fontSize = 14.sp,
                color = if (bio.isNotEmpty()) Color(0xFF424242) else TimelineColors.Axis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(18.dp))

            // J/P 성향
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("여행 성향", fontSize = 13.sp, color = TimelineColors.SubText, modifier = Modifier.weight(1f))
                    Text("J ${score}점", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                JpBar(score)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("P 즉흥", fontSize = 11.sp, color = TimelineColors.SubText, modifier = Modifier.weight(1f))
                    Text("J 계획", fontSize = 11.sp, color = TimelineColors.SubText)
                }
                Spacer(Modifier.height(10.dp))
                Text(jpLabel(score), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBlack)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { Text("닫기", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center) }

            if (onRemove != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { confirmRemove = true }) {
                    Text("동행에서 내보내기", color = TimelineColors.Error, fontSize = 13.sp)
                }
            }
        }
    }

    if (confirmRemove && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("${member.nickname} 님을 내보낼까요?") },
            text = { Text("이 여행의 동행자 목록에서 빠지고, 일정표를 더 이상 볼 수 없어요. 다시 초대하면 돌아올 수 있어요.") },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) {
                    Text("내보내기", color = TimelineColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("취소") } },
        )
    }
}

/** P(초록) ─────●───── J(하늘) 막대. ● 위치 = 점수 */
@Composable
private fun JpBar(score: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
        val dot = 18.dp
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(PColor, JColor))),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - dot) * (score / 100f))
                .size(dot)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, CardBlack, CircleShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 600, name = "프로필 카드")
@Composable
private fun MemberProfilePreview() {
    MemberProfileDialog(Member("u_minji", "민지", "@minji_j", jpScore = 82, bio = "맛집 지도 들고 다니는 사람"), onDismiss = {})
}
