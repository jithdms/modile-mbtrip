package com.example.mbtrip.timeline.ui

import com.example.mbtrip.session.TripSession
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
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.mbtrip.timeline.data.MemberRepository
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.fake.FakeBlocks
import kotlinx.coroutines.launch
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

/**
 * 맨 위 프로필 줄: [+] (사진) (사진) (사진) — 일정표와 숙소·교통 화면이 같이 쓴다.
 *  - [+]   → 친구 초대 모달 (이 줄이 직접 띄운다. onInviteClick 은 쓰지 않음)
 *  - 사진  → 정보 카드 (+ 동행에서 내보내기)
 *
 * @param members        null 이면 저장소에서 동행자를 직접 읽는다 (보통 null 로 두면 됨)
 * @param onInviteClick  예전 호환용 — 더 이상 부르지 않는다 (NavGraph 가 친구 목록으로 넘겨버리는 문제 때문)
 * @param onInvited      초대가 끝난 뒤 알림. null 이면 토스트
 * @param onRemoveMember 내보내기를 직접 처리할 때. null 이면 저장소에서 바로 뺀다
 */
@Composable
fun MemberRow(
    members: List<Member>? = null,
    onInviteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    // TODO(로그인 붙인 뒤): 방장만 남을 내보낼 수 있게, 본인 카드는 '여행 나가기' 로 바꾸기
    onRemoveMember: ((Member) -> Unit)? = null,
    tripId: String = TripSession.currentTripId,
    memberRepository: MemberRepository = TimelineDependencies.memberRepository,
    onInvited: ((List<Member>) -> Unit)? = null,
    /** 프로필 사진 크기 — 일정표 맨 위에 있을 땐 크게, 내리면 기본 */
    avatarSize: Dp = 44.dp,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fromRepo by remember(memberRepository, tripId) { memberRepository.observeMembers(tripId) }
        .collectAsState(initial = emptyList())
    val shown = members ?: fromRepo

    var viewing by remember { mutableStateOf<Member?>(null) }
    var inviting by remember { mutableStateOf(false) }

    // 초대해서 사람이 많아지면 옆으로 밀어서 보기
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(avatarSize).clip(CircleShape)
                .border(1.dp, TimelineColors.Border, CircleShape)
                .background(Color(0xFFF7F7F7))
                // 다른 화면(친구 목록)으로 넘어가지 않고 이 자리에서 초대 모달만 띄운다
                .clickable { inviting = true },
            contentAlignment = Alignment.Center,
        ) { Text("+", fontSize = 24.sp, color = TimelineColors.SubText) }
        shown.forEach { m ->
            // 누르면 그 사람 정보 카드
            MemberAvatar(m, size = avatarSize, modifier = Modifier.clip(CircleShape).clickable { viewing = m })
        }
    }

    if (inviting) {
        InviteDialog(
            tripId = tripId,
            currentMembers = shown,
            repository = memberRepository,
            onDismiss = { inviting = false },
            onInvited = { added ->
                inviting = false
                if (onInvited != null) {
                    onInvited(added)
                } else {
                    Toast.makeText(context, added.joinToString(", ") { it.nickname } + " 님을 초대했어요.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    viewing?.let { m ->
        MemberProfileDialog(
            member = m,
            onDismiss = { viewing = null },
            // 마지막 한 명은 내보낼 수 없게
            onRemove = if (shown.size > 1) {
                {
                    viewing = null
                    if (onRemoveMember != null) {
                        onRemoveMember(m)
                    } else {
                        scope.launch {
                            runCatching { memberRepository.remove(tripId, m.userId) }
                                .onSuccess { Toast.makeText(context, "${m.nickname} 님을 내보냈어요.", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, "내보내지 못했어요: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            } else {
                null
            },
        )
    }
}
