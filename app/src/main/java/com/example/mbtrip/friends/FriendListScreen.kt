package com.example.mbtrip.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mbtrip.timeline.model.Member
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    friends: List<Member>,
    onBack: () -> Unit,
    onFriendClick: (Member) -> Unit = {},
    onDeleteFriend: suspend (Member) -> Unit = {},
) {
    val visibleFriends = remember(friends) { friends.toMutableStateList() }
    var query by rememberSaveable { mutableStateOf("") }
    var activeMenuId by remember { mutableStateOf<String?>(null) }
    var friendForInfo by remember { mutableStateOf<Member?>(null) }
    var friendForDelete by remember { mutableStateOf<Member?>(null) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val filteredFriends = visibleFriends.filter { friend ->
        query.isBlank() || friend.nickname.contains(query, ignoreCase = true) ||
            friend.handle.contains(query, ignoreCase = true) || friend.userId.contains(query, ignoreCase = true)
    }

    Scaffold(
        containerColor = Color(0xFF202020),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("친구 리스트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF202020),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            FriendSearchBar(
                query = query,
                onQueryChange = { query = it },
                focusManager = focusManager,
            )
            Divider(color = Color(0xFFB8B8B8))

            if (filteredFriends.isEmpty()) {
                Text(
                    text = if (visibleFriends.isEmpty()) "친구가 아직 없어요." else "검색 결과가 없어요.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    color = Color(0xFF777777),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredFriends, key = Member::userId) { friend ->
                        FriendRow(
                            friend = friend,
                            menuExpanded = activeMenuId == friend.userId,
                            onRowClick = { onFriendClick(friend); friendForInfo = friend },
                            onMenuClick = { activeMenuId = friend.userId },
                            onMenuDismiss = { activeMenuId = null },
                            onInfoClick = { activeMenuId = null; friendForInfo = friend },
                            onDeleteClick = { activeMenuId = null; friendForDelete = friend },
                        )
                        Divider(color = Color(0xFFB8B8B8))
                    }
                }
            }
        }
    }

    friendForInfo?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendForInfo = null },
            title = { Text("친구 정보") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(friend.nickname, style = MaterialTheme.typography.titleMedium)
                    Text(friend.handle, color = Color(0xFF777777))
                    Text("사용자 ID: ${friend.userId}")
                    Text("J ${100 - friend.jpScore}% · P ${friend.jpScore}%")
                    if (friend.isTraveling) Text("✈ 여행 중")
                }
            },
            confirmButton = {
                TextButton(onClick = { friendForInfo = null }) { Text("닫기") }
            },
        )
    }

    friendForDelete?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendForDelete = null },
            title = { Text("친구 삭제") },
            text = { Text("${friend.nickname}님을 친구 목록에서 삭제할까요?") },
            dismissButton = {
                TextButton(onClick = { friendForDelete = null }) { Text("취소") }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            onDeleteFriend(friend)
                            visibleFriends.removeAll { it.userId == friend.userId }
                            friendForDelete = null
                            snackbarHostState.showSnackbar("친구를 삭제했어요.")
                        } catch (error: Exception) {
                            snackbarHostState.showSnackbar("삭제에 실패했어요. 다시 시도해 주세요.")
                        }
                    }
                }) { Text("삭제") }
            },
        )
    }
}

@Composable
private fun FriendSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusManager: FocusManager,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("검색어 입력") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        IconButton(onClick = { focusManager.clearFocus() }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Search, contentDescription = "친구 검색", modifier = Modifier.size(30.dp), tint = Color.Black)
        }
    }
}

@Composable
private fun FriendRow(
    friend: Member,
    menuExpanded: Boolean,
    onRowClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onRowClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Surface(
                Modifier.size(64.dp).clip(CircleShape),
                color = Color(0xFFE5E7E8),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(friend.nickname.take(1), style = MaterialTheme.typography.titleLarge, color = Color(0xFF525252))
                }
            }
            if (friend.isTraveling) {
                Text("✈", Modifier.align(Alignment.BottomEnd), color = Color.Black)
            }
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(friend.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(friend.handle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A9A9A))
        }
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "친구 메뉴", tint = Color.Black)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                DropdownMenuItem(text = { Text("친구 정보") }, onClick = onInfoClick)
                DropdownMenuItem(text = { Text("친구 삭제") }, onClick = onDeleteClick)
            }
        }
    }
}
