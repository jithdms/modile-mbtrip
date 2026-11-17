package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** 맨 위 프로필 줄: [+] (사진) (사진) (사진) — 일정표와 숙소·교통 화면이 같이 쓴다 */
@Composable
fun MemberRow(members: List<Member>, onInviteClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .border(1.dp, TimelineColors.Border, CircleShape)
                .background(Color(0xFFF7F7F7))
                .clickable(onClick = onInviteClick),
            contentAlignment = Alignment.Center,
        ) { Text("+", fontSize = 24.sp, color = TimelineColors.SubText) }
        members.forEach { MemberAvatar(it) }
    }
}
