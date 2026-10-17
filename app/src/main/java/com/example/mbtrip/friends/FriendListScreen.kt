package com.example.mbtrip.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mbtrip.timeline.model.Member

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun FriendListScreen(
    friends: List<Member>,
    onBack: () -> Unit,
    onFriendClick: (Member) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleFriends = friends.filter {
        it.nickname.contains(query, ignoreCase = true) || it.handle.contains(query, ignoreCase = true)
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("친구") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                singleLine = true,
                placeholder = { Text("이름 또는 아이디 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            if (visibleFriends.isEmpty()) {
                Text(if (friends.isEmpty()) "아직 친구가 없어요." else "검색 결과가 없어요.",
                    modifier = Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(visibleFriends, key = Member::userId) { friend ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onFriendClick(friend) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(Modifier.size(44.dp).clip(CircleShape), color = MaterialTheme.colorScheme.surfaceVariant) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Text(friend.nickname.take(1))
                                }
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(friend.nickname, style = MaterialTheme.typography.titleMedium)
                                Text(friend.handle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("J ${100 - friend.jpScore}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
