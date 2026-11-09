package com.example.mbtrip.timeline.ui

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
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Member
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val InviteBg = Color(0xFFF4F4F4)
private val InviteBlack = Color(0xFF1E1E1E)
private val RowLine = Color(0xFFBDBDBD)
private val PickedBg = Color(0xFFE3F1FB) // 고른 줄 = 확정 블록 하늘색을 연하게

/**
 * 와이어프레임 '방 생성 페이지2 - 친구 초대': 상단 `+` 를 누르면 뜨는 모달.
 *  - 검색창: 닉네임 또는 @아이디 (글자를 칠 때마다 찾음)
 *  - 목록: 사진 · 닉네임 · @아이디. 누르면 고르기/풀기 (고른 사람은 사진에 ✈ 표시)
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

    // 글자를 칠 때마다 0.25초 기다렸다가 찾기 (한 글자마다 서버에 묻지 않게)
    LaunchedEffect(query) {
        results = null
        delay(250)
        results = runCatching { repository.searchUsers(query) }
            .onFailure { error = "검색하지 못했어요: ${it.message}" }
            .getOrDefault(emptyList())
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .imePadding()
                .clip(RoundedCornerShape(20.dp))
                .background(InviteBg)
                .padding(vertical = 16.dp),
        ) {
            // 검색창
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; error = null },
                singleLine = true,
                placeholder = { Text("검색어 입력", fontSize = 16.sp, color = TimelineColors.Axis) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = InviteBlack,
                    unfocusedBorderColor = Color(0xFF555555),
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
                    list.isEmpty() -> CenterNote(if (query.isBlank()) "닉네임이나 @아이디로 찾아보세요." else "'$query' 와(과) 맞는 사람이 없어요.")
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
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (canSend) InviteBlack else Color(0xFFD6D6D6))
                        .clickable(enabled = canSend) {
                            scope.launch {
                                sending = true
                                runCatching { repository.invite(tripId, picked) }
                                    .onSuccess { onInvited(picked) }
                                    .onFailure { error = "초대하지 못했어요: ${it.message}" }
                                sending = false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            sending -> "초대하는 중…"
                            picked.isEmpty() -> "초대하기"
                            else -> "초대하기 (${picked.size})"
                        },
                        color = Color.White, fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRow(member: Member, joined: Boolean, picked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (picked) PickedBg else Color.Transparent)
            .clickable(enabled = !joined, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            MemberAvatar(member, size = 60.dp)
            if (picked) {
                // 와이어프레임의 ✈ 표시 = 고른 사람
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, InviteBlack, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("✈", fontSize = 13.sp, color = InviteBlack) }
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(member.nickname, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (joined) TimelineColors.SubText else Color.Black)
            Text(member.handle, fontSize = 12.sp, color = TimelineColors.Axis)
        }
        when {
            joined -> Text("동행 중", fontSize = 12.sp, color = TimelineColors.SubText)
            picked -> Text("선택됨", fontSize = 12.sp, color = Color(0xFF1E88E5), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CenterNote(text: String) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = TimelineColors.SubText)
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
