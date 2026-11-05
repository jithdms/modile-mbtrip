package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.R
import com.example.mbtrip.timeline.model.Member

/**
 * 동그란 프로필 사진.
 * 사진을 등록하지 않은 사람(photoUrl == null)은 기본 사진(res/drawable/default_profile.jpg).
 *
 * TODO(로그인·사진 업로드 붙일 때): photoUrl 이 있으면 인터넷 사진을 보여줘야 함.
 *   → 이미지 라이브러리(Coil) 추가 후 AsyncImage(model = member.photoUrl, ...) 로 바꾸기.
 *   지금은 photoUrl 이 있어도 기본 사진을 보여준다.
 */
@Composable
fun MemberAvatar(member: Member, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.default_profile),
        contentDescription = "${member.nickname} 프로필",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, TimelineColors.Border, CircleShape),
    )
}

/** 맨 위 프로필 줄: [+] (사진) (사진) (사진) — 일정표와 숙소·교통 화면이 같이 쓴다. 사진을 누르면 정보 카드 */
@Composable
fun MemberRow(
    members: List<Member>,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * 정보 카드의 [동행에서 내보내기]. null 이면 버튼 없음.
     * TODO(로그인 붙인 뒤): 방장만 남을 내보낼 수 있게, 본인 카드는 '여행 나가기' 로 바꾸기
     */
    onRemoveMember: ((Member) -> Unit)? = null,
) {
    var viewing by remember { mutableStateOf<Member?>(null) }
    // 초대해서 사람이 많아지면 옆으로 밀어서 보기
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .border(1.dp, TimelineColors.Border, CircleShape)
                .background(Color(0xFFF7F7F7))
                .clickable(onClick = onInviteClick),
            contentAlignment = Alignment.Center,
        ) { Text("+", fontSize = 24.sp, color = TimelineColors.SubText) }
        members.forEach { m ->
            // 누르면 그 사람 정보 카드
            MemberAvatar(m, modifier = Modifier.clip(CircleShape).clickable { viewing = m })
        }
    }

    viewing?.let { m ->
        MemberProfileDialog(
            member = m,
            onDismiss = { viewing = null },
            // 마지막 한 명은 내보낼 수 없게
            onRemove = if (onRemoveMember != null && members.size > 1) {
                { viewing = null; onRemoveMember(m) }
            } else {
                null
            },
        )
    }
}
