package com.example.mbtrip.createtrip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.example.mbtrip.diagnosis.GroupRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.session.TripSession
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.ui.BackButton
import com.example.mbtrip.timeline.ui.MemberRow
import java.time.format.DateTimeFormatter

private val Black = Color(0xFF1E1E1E)
private val Sub = Color(0xFF757575)
private val DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 방 만들기 → [생성하기] 다음: 같이 갈 친구 초대.
 *  - 프로필 줄의 [+] → 친구 검색·초대 모달 (일정표와 같은 부품)
 *  - 사진 누르기 → 정보 카드
 *  - 아래: 동행자 성향으로 계산한 적정 확정 · 자유 비율 (GroupRatio)
 *  - 아래: 초대코드 (영어 대소문자 + 숫자 6자리) — 내가 초대하지 않아도 코드로 들어올 수 있다
 *  - [다음] → 숙소·교통 입력. 친구는 나중에 일정표에서도 초대할 수 있다.
 */
@Composable
fun InviteStepScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val tripId = TripSession.currentTripId
    val repo = TimelineDependencies.memberRepository
    val members by remember(tripId) { repo.observeMembers(tripId) }.collectAsState(initial = emptyList())
    val planned = TripSession.headcount
    val ratio = GroupRatio.of(members.map { it.jpScore })
    val code = TripSession.inviteCode
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("친구 초대", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("함께 갈 친구를 초대해요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Black)
            val start = TripSession.startDate
            val end = TripSession.endDate
            Text(
                buildString {
                    append(TripSession.destination)
                    if (start != null && end != null) append(" · ${start.format(DATE)} ~ ${end.format(DATE)}")
                },
                fontSize = 14.sp, color = Sub,
            )
            Spacer(Modifier.height(16.dp))

            // 지금 동행자 + [+] (누르면 검색·초대 모달)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF4F4F4))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (planned != null) "동행자 ${members.size} / ${planned}명" else "동행자 ${members.size}명",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    MemberRow(members = members, tripId = tripId, memberRepository = repo, avatarSize = 56.dp)
                    Text("+ 를 눌러 닉네임이나 @아이디로 친구를 찾아 초대하세요.", fontSize = 12.sp, color = Sub)
                }
            }

            // 적정 확정 · 자유 비율
            if (ratio != null) RatioCard(ratio)

            // 초대코드
            CodeCard(
                code = code,
                onCopy = {
                    if (code != null) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("MBTrip 초대코드", code))
                        Toast.makeText(context, "초대코드를 복사했어요.", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    if (code != null) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "MBTrip ${TripSession.destination} 여행에 같이 가요! 앱에서 [초대코드로 참여하기] → $code",
                            )
                        }
                        context.startActivity(Intent.createChooser(send, "초대코드 보내기"))
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        // 다음
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Black)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) { Text("다음", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(6.dp))
            Text("친구는 나중에 일정표에서도 초대할 수 있어요.", fontSize = 12.sp, color = Sub)
        }
    }
}

/** 동행자 성향으로 계산한 적정 비율 */
@Composable
private fun RatioCard(ratio: GroupRatio) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Color(0xFFE0E0E0), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("우리에게 맞는 일정 비율", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("확정 ${ratio.fixedPercent}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E86C1))
            Text("  ·  ", fontSize = 16.sp, color = Sub)
            Text("자유 ${ratio.freePercent}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F9A47))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(FreeGreen),
        ) {
            Box(Modifier.fillMaxWidth(ratio.fixedPercent / 100f).fillMaxHeight().background(FixedBlue))
        }
        Text(ratio.reason, fontSize = 12.sp, color = Sub)
    }
}

/** 초대코드 — 크게 보여주고 복사·보내기 */
@Composable
private fun CodeCard(code: String?, onCopy: () -> Unit, onShare: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFF4F4F4))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("초대코드", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            code ?: "만드는 중…",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            color = Black,
        )
        Text("친구가 앱에서 [초대코드로 참여하기]에 이 코드를 넣으면 바로 들어와요. 대소문자를 구분해요.", fontSize = 12.sp, color = Sub)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallPill("복사", enabled = code != null, onClick = onCopy)
            SmallPill("보내기", enabled = code != null, onClick = onShare)
        }
    }
}

@Composable
private fun SmallPill(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        fontSize = 13.sp,
        color = if (enabled) Black else Color(0xFFBDBDBD),
        modifier = Modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, if (enabled) Black else Color(0xFFD6D6D6), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

private val FixedBlue = Color(0xFF5DADE2)
private val FreeGreen = Color(0xFFA9E5A0)
