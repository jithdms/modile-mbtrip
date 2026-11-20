package com.example.mbtrip.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.R
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.theme.Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.ui.MemberAvatar
import com.example.mbtrip.timeline.ui.MemberProfileDialog
import androidx.compose.runtime.remember
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SkyBackground
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.design.mbFieldColors
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Mint
import com.example.mbtrip.ui.theme.MintDeep
import com.example.mbtrip.ui.theme.Muted

/**
 * 친구 목록 — 검색창 + 카드 목록.
 *  - 오른쪽 위 + → 친구 추가 모달 (아이디·이름 검색, 함께 여행한 사람 추천)
 *  - 친구를 누르면 상세 정보 모달 → 아래 [친구 삭제]
 * 처음 열면 카드가 차례로 나타나고, 검색하면 걸러진 카드가 제자리로 부드럽게 옮겨 간다.
 */
@Composable
fun FriendListScreen(
    onBack: () -> Unit,
    onFriendClick: (Member) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val friends = Friends.list
    var query by rememberSaveable { mutableStateOf("") }
    // 누른 친구 → 상세 정보 모달
    var viewing by remember { mutableStateOf<Member?>(null) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { Friends.refresh() } }

    val visibleFriends = friends.filter {
        it.nickname.contains(query, ignoreCase = true) || it.handle.contains(query, ignoreCase = true)
    }
    SkyBackground(Modifier.fillMaxSize(), intensity = 0.7f) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "친구",
                subtitle = if (Friends.loaded) "함께 여행할 친구 ${friends.size}명" else "불러오는 중…",
                onBack = onBack,
                actions = {
                    // 배경 없이 검은 + 만
                    Box(
                        Modifier
                            .size(44.dp)
                            .bouncyClick(CircleShape) { adding = true },
                        contentAlignment = Alignment.Center,
                    ) { MBIcon(R.drawable.ic_mb_add, size = 28.dp, tint = Ink, contentDescription = "친구 추가") }
                },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .enterFromBelow(1),
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = mbFieldColors(),
                placeholder = { Text("이름 또는 아이디 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            AnimatedContent(
                targetState = visibleFriends.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "friendsEmpty",
            ) { empty ->
                if (empty) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 64.dp, start = 32.dp, end = 32.dp).enterPop(0, from = 0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MBIcon(if (friends.isEmpty()) R.drawable.ic_mb_group else R.drawable.ic_mb_search, size = 32.dp, tint = Muted)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (friends.isEmpty()) "아직 친구가 없어요." else "검색 결과가 없어요.",
                            style = MaterialTheme.typography.titleSmall, color = Muted,
                        )
                        if (friends.isEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            MBButton(
                                text = "친구 추가하기",
                                onClick = { adding = true },
                                style = MBButtonStyle.Outline,
                                height = 44.dp,
                                leadingIcon = R.drawable.ic_mb_add,
                            )
                        }
                    }
                } else {
                    // 가로를 꽉 채운 줄 — 위아래 얇은 선으로만 구분
                    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                        item(key = "top_line") { HorizontalDivider(color = Line) }
                        itemsIndexed(visibleFriends, key = { _, f -> f.userId }) { i, friend ->
                            Column(
                                Modifier
                                    .animateItem()
                                    .enterFromBelow(i + 2, distance = 16.dp, delayStepMs = 45),
                            ) {
                                FriendCard(friend) {
                                    viewing = friend
                                    onFriendClick(friend)
                                }
                                HorizontalDivider(color = Line)
                            }
                        }
                    }
                }
            }
        }
    }
    viewing?.let { m ->
        MemberProfileDialog(
            member = m,
            onDismiss = { viewing = null },
            onRemove = {
                scope.launch {
                    val problem = Friends.remove(m.userId)
                    Toast.makeText(context, problem ?: "${m.nickname} 님을 친구에서 삭제했어요.", Toast.LENGTH_SHORT).show()
                    if (problem == null) viewing = null
                }
            },
            removeLabel = "친구 삭제",
            removeConfirmTitle = "${m.nickname} 님을 친구에서 삭제할까요?",
            removeConfirmText = "서로의 친구 목록에서 빠지고, 여행에 초대할 수 없게 돼요. 함께하던 여행은 그대로 남아요.",
            removeConfirmButton = "삭제",
        )
    }
    if (adding) {
        AddFriendDialog(onDismiss = { adding = false })
    }
}

/**
 * 친구 추가 모달 — 이름·@아이디로 가입한 사람을 찾아 [추가].
 * 검색칸이 비어 있으면 함께 여행한 사람 중 아직 친구가 아닌 사람을 보여준다.
 */
@Composable
private fun AddFriendDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Member>?>(null) } // null = 찾는 중
    var mates by remember { mutableStateOf<List<Member>>(emptyList()) }
    var busyId by remember { mutableStateOf<String?>(null) }

    val tester = com.example.mbtrip.session.TestAccess.isTester
    val sent = FriendRequests.sentPending
    LaunchedEffect(Unit) {
        if (!tester) runCatching { FriendRequests.syncAnswers() } // '신청함' 표시용
        mates = runCatching { Friends.travelMates() }.getOrDefault(emptyList())
    }
    LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); return@LaunchedEffect }
        results = null
        delay(250) // 한 글자마다 서버에 묻지 않게
        results = runCatching { Friends.searchPeople(query) }.getOrDefault(emptyList())
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .imePadding()
                .enterPop(0, from = 0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 18.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("친구 신청", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).bouncyClick(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_close, size = 18.dp, tint = Muted, contentDescription = "닫기") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = mbFieldColors(),
                placeholder = { Text("이름 또는 @아이디") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Line)

            val showing: List<Member>? = if (query.isBlank()) mates else results
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (query.isBlank() && mates.isNotEmpty()) {
                    Text(
                        "함께 여행한 사람", fontSize = 12.sp, color = Muted,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                when {
                    showing == null -> Note("찾는 중…")
                    showing.isEmpty() -> Note(
                        if (query.isBlank()) "친구의 이름이나 @아이디를 검색해 보세요." else "'$query' 와(과) 맞는 사람이 없어요.",
                    )
                    else -> showing.forEach { m ->
                        val already = Friends.isFriend(m.userId)
                        val requested = m.userId in sent
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MemberAvatar(m, size = 44.dp)
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(m.nickname, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(m.handle, fontSize = 12.sp, color = Muted)
                            }
                            val shape = RoundedCornerShape(50)
                            Text(
                                when {
                                    already -> "친구"
                                    requested -> "신청함"
                                    busyId == m.userId -> "보내는 중"
                                    else -> "친구 신청"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (already || requested) Muted else Color.White,
                                modifier = Modifier
                                    .bouncyClick(shape, enabled = !already && !requested && busyId == null) {
                                        scope.launch {
                                            busyId = m.userId
                                            // 테스트 계정은 바로 친구, 그 밖에는 신청 → 상대가 알림에서 수락
                                            val problem = if (tester) Friends.add(m) else FriendRequests.send(m)
                                            busyId = null
                                            Toast.makeText(
                                                context,
                                                problem ?: if (tester) "${m.nickname} 님과 친구가 됐어요."
                                                else "${m.nickname} 님에게 친구 신청을 보냈어요. 수락하면 친구 목록에 보여요.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                    .background(if (already || requested) Color.White else Ink, shape)
                                    .border(Hairline, if (already || requested) Line else Ink, shape)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                        HorizontalDivider(color = Line, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Box(Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FriendCard(friend: Member, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val j = 100 - friend.jpScore
    val isJ = j >= 50
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick) // 모서리 없는 줄 전체가 눌린다
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(friend, size = 48.dp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(friend.nickname, style = MaterialTheme.typography.titleMedium, color = Ink)
            val trip = Friends.travelingTo(friend.userId)
            if (trip != null) {
                Text(
                    "✈ ${trip.ifBlank { "여행" }} 여행 중",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Aqua700,
                )
            } else {
                Text(friend.handle, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.width(8.dp))
        // J/P 성향 배지 — J 면 아쿠아, P 면 민트
        Row(
            Modifier
                .background(if (isJ) Aqua100 else Mint.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(if (isJ) Aqua500 else MintDeep, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                "J $j%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isJ) Aqua700 else MintDeep,
            )
        }
    }
}
