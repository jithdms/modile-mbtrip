package com.example.mbtrip.createtrip

import androidx.compose.ui.graphics.SolidColor
import com.example.mbtrip.R
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.mbtrip.ui.design.FillBar
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.animatedCount
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.MBBrush
import com.example.mbtrip.ui.theme.Mint
import com.example.mbtrip.ui.theme.MintDeep
import com.example.mbtrip.ui.theme.Muted
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.ui.BackButton
import com.example.mbtrip.timeline.ui.MemberRow
import java.time.format.DateTimeFormatter

private val Black = Ink
private val Sub = Muted
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

    SkyBackground(Modifier.fillMaxSize(), intensity = 0.7f) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "친구 초대", onBack = onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.enterFromBelow(1)) {
                Column(Modifier.weight(1f)) {
                    Text("함께 갈 친구를 초대해요", style = MaterialTheme.typography.headlineSmall, color = Black)
                    Spacer(Modifier.height(4.dp))
                    val start = TripSession.startDate
                    val end = TripSession.endDate
                    Text(
                        buildString {
                            append(TripSession.destination)
                            if (start != null && end != null) append(" · ${start.format(DATE)} ~ ${end.format(DATE)}")
                        },
                        fontSize = 14.sp, color = Sub,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // 지금 동행자 + [+] (누르면 검색·초대 모달)
            MBCard(Modifier.fillMaxWidth().enterFromBelow(2, distance = 36.dp), contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("동행자", icon = R.drawable.ic_mb_group, modifier = Modifier.weight(1f))
                        SoftBadge(if (planned != null) "${members.size} / ${planned}명" else "${members.size}명")
                    }
                    // 예정 인원 중 몇 명이 들어왔는지
                    if (planned != null && planned > 0) {
                        FillBar(members.size.toFloat() / planned, height = 6.dp)
                    }
                    MemberRow(members = members, tripId = tripId, memberRepository = repo, avatarSize = 56.dp)
                    Text("+ 를 눌러 닉네임이나 @아이디로 친구를 찾아 초대하세요.", fontSize = 12.sp, color = Sub)
                }
            }

            // 적정 확정 · 자유 비율
            if (ratio != null) Box(Modifier.enterFromBelow(3, distance = 36.dp)) { RatioCard(ratio) }

            // 초대코드
            Box(Modifier.enterFromBelow(4, distance = 36.dp)) {
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
            }
            Spacer(Modifier.height(8.dp))
        }

        // 다음
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp).enterFromBelow(5, distance = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MBButton(text = "다음", onClick = onNext, style = MBButtonStyle.Brand)
            Spacer(Modifier.height(8.dp))
            Text("친구는 나중에 일정표에서도 초대할 수 있어요.", fontSize = 12.sp, color = Sub)
        }
    }
    }
}

/** 동행자 성향으로 계산한 적정 비율 — 숫자는 올라가고 막대는 차오른다 */
@Composable
private fun RatioCard(ratio: GroupRatio) {
    MBCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("우리에게 맞는 일정 비율", icon = R.drawable.ic_mb_ratio)
        val fixed by animatedCount(ratio.fixedPercent)
        val free by animatedCount(ratio.freePercent)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("확정 $fixed%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FixedText)
            Text("  ·  ", fontSize = 16.sp, color = Faint)
            Text("자유 $free%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MintDeep)
        }
        FillBar(
            ratio.fixedPercent / 100f,
            height = 12.dp,
            track = FreeGreen,
            fill = SolidColor(FixedBlue),
        )
        Text(ratio.reason, fontSize = 12.sp, color = Sub, lineHeight = 18.sp)
    }
}

/** 초대코드 — 한 글자씩 톡톡 튀어나오고 복사·보내기 */
@Composable
private fun CodeCard(code: String?, onCopy: () -> Unit, onShare: () -> Unit) {
    MBCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("초대코드", icon = R.drawable.ic_mb_key)
        if (code == null) {
            Text("만드는 중…", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Faint)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                code.forEachIndexed { i, ch ->
                    Box(
                        Modifier
                            .enterPop(i, delayStepMs = 80, startDelayMs = 350, from = 0.3f)
                            .size(width = 40.dp, height = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Aqua300, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ch.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Black,
                        )
                    }
                }
            }
        }
        Text("친구가 앱에서 [초대코드로 참여하기]에 이 코드를 넣으면 바로 들어와요. 대소문자를 구분해요.", fontSize = 12.sp, color = Sub, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallPill("복사", R.drawable.ic_mb_copy, enabled = code != null, onClick = onCopy)
            SmallPill("보내기", R.drawable.ic_mb_share, enabled = code != null, onClick = onShare)
        }
    }
}

@Composable
private fun SmallPill(text: String, icon: Int, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .bouncyClick(shape, enabled = enabled, onClick = onClick)
            .background(Color.White, shape)
            .border(1.dp, Line, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.example.mbtrip.ui.design.MBIcon(icon, size = 16.dp, tint = if (enabled) Black else Faint)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) Black else Faint)
    }
}

private val FixedBlue = Aqua500
private val FixedText = Aqua700
private val FreeGreen = Mint
