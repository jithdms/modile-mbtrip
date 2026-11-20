package com.example.mbtrip.timeline.ui

import com.example.mbtrip.ui.theme.Muted
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.R
import com.example.mbtrip.ui.design.bouncyClick
import androidx.compose.ui.draw.shadow
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

private val CardBg = Aqua50
private val CardBlack = Ink
private val PColor = Mint // P = 자유 블록 초록
private val JColor = Aqua300 // J = 확정 블록 하늘

/** jpScore(0 = J ~ 100 = P, 데이터 구조 문서 기준) → 한 줄 설명 */
internal fun jpLabel(score: Int): String = when {
    score <= 20 -> "확실한 J · 계획형"
    score < 40 -> "J 쪽 · 계획을 좋아해요"
    score < 60 -> "J·P 반반 · 상황에 맞춰요"
    score < 80 -> "P 쪽 · 즉흥을 좋아해요"
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
    /** 아래 빨간 버튼 글자 · 확인 창 문구 (친구 목록에서는 '친구 삭제') */
    removeLabel: String = "동행에서 내보내기",
    removeConfirmTitle: String = "${member.nickname} 님을 내보낼까요?",
    removeConfirmText: String = "이 여행의 동행자 목록에서 빠지고, 일정표를 더 이상 볼 수 없어요. 다시 초대하면 돌아올 수 있어요.",
    removeConfirmButton: String = "내보내기",
) {
    val score = member.jpScore.coerceIn(0, 100)
    var confirmRemove by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 360.dp)
                .enterPop(0, from = 0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 닫기
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(32.dp).bouncyClick(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_close, size = 18.dp, tint = Muted, contentDescription = "닫기") }
            }

            // 프로필 사진
            MemberAvatar(member, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text(member.nickname, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(member.handle, fontSize = 13.sp, color = TimelineColors.SubText)
            // 여행 중인 친구
            com.example.mbtrip.friends.Friends.travelingTo(member.userId)?.let { trip ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "✈ ${trip.ifBlank { "여행" }} 여행 중",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Aqua700,
                    modifier = Modifier
                        .background(com.example.mbtrip.ui.theme.Aqua100, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }

            // 한 줄 소개 (본인이 적은 글)
            Spacer(Modifier.height(12.dp))
            val bio = member.bio?.trim().orEmpty()
            Text(
                if (bio.isNotEmpty()) "“${bio}”" else "아직 한 줄 소개가 없어요.",
                fontSize = 14.sp,
                color = if (bio.isNotEmpty()) InkSoft else TimelineColors.Axis,
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
                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("여행 성향", fontSize = 13.sp, color = TimelineColors.SubText, modifier = Modifier.weight(1f))
                    Text("J ${100 - score}% · P ${score}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Spacer(Modifier.height(10.dp))
                JpBar(score)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("J 계획", fontSize = 11.sp, color = TimelineColors.SubText, modifier = Modifier.weight(1f))
                    Text("P 즉흥", fontSize = 11.sp, color = TimelineColors.SubText)
                }
                Spacer(Modifier.height(10.dp))
                Text(jpLabel(score), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Aqua700)
            }

            Spacer(Modifier.height(16.dp))
            com.example.mbtrip.ui.design.MBButton(
                text = "닫기",
                onClick = onDismiss,
                style = com.example.mbtrip.ui.design.MBButtonStyle.Deep,
                height = 48.dp,
            )

            if (onRemove != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { confirmRemove = true }) {
                    Text(removeLabel, color = TimelineColors.Error, fontSize = 13.sp)
                }
            }
        }
    }

    if (confirmRemove && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(removeConfirmTitle) },
            text = { Text(removeConfirmText) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) {
                    Text(removeConfirmButton, color = TimelineColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("취소") } },
        )
    }
}

/** J(하늘) ─────●───── P(초록) 막대. ● 위치 = jpScore (0 = 왼쪽 J 끝, 100 = 오른쪽 P 끝) */
@Composable
private fun JpBar(score: Int) {
    // 처음 열면 점이 J 끝에서 제자리로 미끄러져 간다
    val pos by com.example.mbtrip.ui.design.animatedFill(score / 100f, durationMs = 900, delayMs = 300)
    BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
        val dot = 18.dp
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(JColor, PColor))),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - dot) * pos)
                .size(dot)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Aqua500, CircleShape),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 600, name = "프로필 카드")
@Composable
private fun MemberProfilePreview() {
    MemberProfileDialog(Member("u_minji", "민지", "@minji_j", jpScore = 18, bio = "맛집 지도 들고 다니는 사람"), onDismiss = {})
}
