package com.example.mbtrip.timeline.ui

import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.R
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.timeline.data.FakeMemberRepository
import com.example.mbtrip.timeline.data.MemberRepository
import com.example.mbtrip.timeline.data.normalizeQuery
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val InviteBg = Aqua50
private val InviteBlack = Ink
private val RowLine = Line
private val PickedBg = Aqua100 // 고른 줄 = 확정 블록 하늘색을 연하게

/**
 * 와이어프레임 '방 생성 페이지2 - 친구 초대': 상단 `+` 를 누르면 뜨는 모달.
 *  - 목록에는 나와 친구인 사람만 나온다 (친구 목록 + 로 먼저 추가)
 *  - 검색창: 친구 중에서 닉네임 또는 @아이디로 거르기
 *  - 목록: 사진 · 닉네임 · @아이디. 누르면 고르기/풀기 (고른 사람은 사진에 체크 표시)
 *  - 이미 동행 중인 사람은 '동행 중' 으로 보이고 못 고른다
 *  - [초대하기] → 고른 사람들을 동행자로 추가
 */
@Composable
internal fun InviteDialog(
    tripId: String,
    currentMembers: List<Member>,
    repository: MemberRepository,
    onDismiss: () -> Unit,
    onInvited: (List<Member>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Member>?>(null) } // null = 찾는 중
    var picked by remember { mutableStateOf<List<Member>>(emptyList()) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val joinedIds = remember(currentMembers) { currentMembers.map { it.userId }.toSet() }

    // 친구 목록을 (아직 안 읽었으면) 읽어 온 뒤, 친구 중에서만 거른다
    val friends = com.example.mbtrip.friends.Friends.list
    val friendsLoaded = com.example.mbtrip.friends.Friends.loaded
    LaunchedEffect(Unit) {
        if (!friendsLoaded) {
            runCatching { com.example.mbtrip.friends.Friends.refresh() }
                .onFailure { error = "친구 목록을 불러오지 못했어요." }
        }
    }
    LaunchedEffect(query, friends, friendsLoaded) {
        if (!friendsLoaded) { results = null; return@LaunchedEffect }
        val q = normalizeQuery(query)
        results = friends.filter {
            q.isEmpty() || it.nickname.lowercase().contains(q) || it.handle.removePrefix("@").lowercase().contains(q)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .imePadding()
                .enterPop(0, from = 0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 16.dp),
        ) {
            // 검색창
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; error = null },
                singleLine = true,
                placeholder = { Text("친구 이름·아이디 검색", fontSize = 16.sp, color = TimelineColors.Axis) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Aqua500,
                    cursorColor = Aqua700,
                    unfocusedBorderColor = Color(0xFFBFD8E0),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RowLine)

            // 목록
            val list = results
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 330.dp)) {
                when {
                    list == null -> CenterNote("찾는 중…")
                    list.isEmpty() -> CenterNote(
                        when {
                            friends.isEmpty() -> "친구로 추가한 사람만 초대할 수 있어요.\n친구 목록 오른쪽 위 + 로 먼저 추가해 주세요."
                            else -> "친구 중에 '$query' 와(과) 맞는 사람이 없어요."
                        },
                    )
                    else -> LazyColumn(Modifier.fillMaxWidth()) {
                        items(list, key = { it.userId }) { m ->
                            val joined = m.userId in joinedIds
                            val isPicked = picked.any { it.userId == m.userId }
                            InviteRow(m, joined, isPicked) {
                                picked = if (isPicked) picked.filterNot { it.userId == m.userId } else picked + m
                            }
                            HorizontalDivider(color = RowLine)
                        }
                    }
                }
            }

            // 고른 사람 요약 · 오류
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                if (picked.isNotEmpty()) {
                    Text(
                        picked.joinToString(", ") { it.nickname } + " 님을 초대해요",
                        fontSize = 12.sp, color = TimelineColors.SubText,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                error?.let {
                    Text(it, fontSize = 12.sp, color = TimelineColors.Error)
                    Spacer(Modifier.height(6.dp))
                }

                // 초대하기
                val canSend = picked.isNotEmpty() && !sending
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bouncyClick(RoundedCornerShape(25.dp), enabled = canSend, elevation = if (canSend) 8.dp else 0.dp, shadowColor = Aqua600) {
                            scope.launch {
                                sending = true
                                runCatching { repository.invite(tripId, picked) }
                                    .onSuccess { onInvited(picked) }
                                    .onFailure { error = "초대하지 못했어요: ${it.message}" }
                                sending = false
                            }
                        }
                        .background(if (canSend) com.example.mbtrip.ui.theme.MBBrush.Primary else androidx.compose.ui.graphics.SolidColor(Line)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            sending -> "초대하는 중…"
                            picked.isEmpty() -> "초대하기"
                            else -> "초대하기 (${picked.size})"
                        },
                        color = if (canSend) Ink else com.example.mbtrip.ui.theme.Faint, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRow(member: Member, joined: Boolean, picked: Boolean, onClick: () -> Unit) {
    val rowBg by androidx.compose.animation.animateColorAsState(if (picked) PickedBg else Color.Transparent, label = "pickRow")
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(enabled = !joined, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            MemberAvatar(member, size = 60.dp)
            if (picked) {
                // 고른 사람 = 사진 오른쪽 아래 체크
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 2.dp)
                        .enterPop(0)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Ink)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_check, size = 14.dp, tint = Color.White, contentDescription = "선택됨") }
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(member.nickname, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (joined) TimelineColors.SubText else Ink)
            Text(member.handle, fontSize = 12.sp, color = TimelineColors.Axis)
        }
        when {
            joined -> Text("동행 중", fontSize = 12.sp, color = TimelineColors.SubText)
            picked -> Text("선택됨", fontSize = 12.sp, color = Aqua700, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CenterNote(text: String) {
    Box(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = TimelineColors.SubText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700, name = "친구 초대 모달")
@Composable
private fun InviteDialogPreview() {
    InviteDialog(
        tripId = FakeBlocks.TRIP_ID,
        currentMembers = FakeBlocks.members,
        repository = FakeMemberRepository(),
        onDismiss = {},
        onInvited = {},
    )
}
