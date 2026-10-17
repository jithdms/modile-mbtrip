package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.model.TimeMarker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private data class EditorState(val block: Block, val isNew: Boolean)

/**
 * P6 일정표 — 가짜 데이터 버전.
 *
 * @param markers 체크인·체크아웃 시각 (파란 선)
 * @param now 현재 시각 표시선 위치 (빨간 선). null 이면 표시하지 않음 (여행 당일에만 넘김).
 * @param onInviteClick 상단 `+` → 이수진(P3) 초대 모달 연결 지점
 * @param onVoteClick   제안됨·투표중 블록의 '투표하기' → 최재성(P7) 투표 모달 연결 지점
 */
@Composable
fun TimelineScreen(
    initialBlocks: List<Block> = FakeBlocks.blocks,
    members: List<Member> = FakeBlocks.members,
    tripId: String = FakeBlocks.TRIP_ID,
    tripStart: LocalDate = FakeBlocks.TRIP_START,
    tripEnd: LocalDate = FakeBlocks.TRIP_END,
    markers: List<TimeMarker> = FakeBlocks.markers,
    now: LocalTime? = null,
    onInviteClick: () -> Unit = {},
    onVoteClick: (Block) -> Unit = {},
) {
    // 2주차에 ViewModel + StateFlow(Firestore 리스너)로 옮길 상태
    var blocks by remember { mutableStateOf(initialBlocks) }
    val dates = remember(tripStart, tripEnd) { tripDates(tripStart, tripEnd) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedDate = dates.getOrNull(selectedIndex) ?: dates.first()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var choosingKind by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            MemberBar(members, onInviteClick)
            HorizontalDivider()
            DayTabs(dates, selectedIndex) { selectedIndex = it }

            val dayBlocks = blocks.filter { it.date == selectedDate }.sortedBy { it.start }
            DayTimelineGrid(
                dayBlocks = dayBlocks,
                markers = markers.filter { it.date == selectedDate },
                now = now,
                onBlockClick = { editor = EditorState(it, isNew = false) },
                onBlockMoved = { block, requested, window ->
                    when (val r = moveBlock(blocks, block.id, requested, window)) {
                        is EditResult.Ok -> {
                            val moved = r.blocks.first { it.id == block.id }
                            if (block.kind == BlockKind.FIXED && block.status == BlockStatus.CONFIRMED &&
                                moved.status == BlockStatus.PROPOSED
                            ) toast("확정 블록을 옮겨서 '제안됨'으로 바뀌었어요.")
                            blocks = r.blocks
                        }
                        is EditResult.Error -> toast(r.message)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        FloatingActionButton(
            onClick = { choosingKind = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            shape = CircleShape,
        ) { Text("+", fontSize = 28.sp) }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (choosingKind) {
        KindChooserDialog(
            onDismiss = { choosingKind = false },
            onChoose = { kind ->
                choosingKind = false
                val draft = newDraft(blocks, tripId, selectedDate, kind)
                if (draft == null) {
                    toast("이 날에는 1시간짜리 빈 시간이 없어요.")
                } else {
                    editor = EditorState(draft, isNew = true)
                }
            },
        )
    }

    editor?.let { st ->
        BlockEditorSheet(
            original = st.block,
            isNew = st.isNew,
            allBlocks = blocks,
            onDismiss = { editor = null },
            onSave = { edited ->
                val r = if (st.isNew) addBlock(blocks, edited) else updateBlock(blocks, st.block, edited)
                when (r) {
                    is EditResult.Ok -> { blocks = r.blocks; editor = null }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onDelete = {
                when (val r = deleteBlock(blocks, st.block.id)) {
                    is EditResult.Ok -> { blocks = r.blocks; editor = null; toast("블록을 삭제했어요.") }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onVoteClick = { onVoteClick(st.block) },
        )
    }
}

/* ───────────────────────── 상단: 동행자 바 ───────────────────────── */

@Composable
private fun MemberBar(members: List<Member>, onInviteClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Circle(onClick = onInviteClick) { Text("+", fontSize = 24.sp, color = TimelineColors.SubText) }
        members.forEach { m -> Circle { Text(m.nickname.take(1), fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun Circle(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .border(1.dp, TimelineColors.Border, CircleShape)
            .background(Color(0xFFF7F7F7))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/* ───────────────────────── 날짜 탭 (1일차, 2일차 …) ───────────────────────── */

@Composable
private fun DayTabs(dates: List<LocalDate>, selected: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected.coerceIn(0, dates.lastIndex),
        edgePadding = 12.dp,
        containerColor = Color.White,
        contentColor = Color.Black,
    ) {
        dates.forEachIndexed { i, date ->
            val isSel = i == selected
            Tab(
                selected = isSel,
                onClick = { onSelect(i) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${i + 1}일차",
                            fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else TimelineColors.SubText,
                        )
                        Text(date.format(MD), fontSize = 11.sp, color = TimelineColors.SubText)
                    }
                },
            )
        }
    }
}

/* ───────────────────────── + 버튼 → 종류 선택 ───────────────────────── */

@Composable
private fun KindChooserDialog(onDismiss: () -> Unit, onChoose: (BlockKind) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("어떤 블록을 추가할까요?", fontSize = 18.sp) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KindOption("확정 블록", "시간·장소·할 일을 정해요\n'제안됨'으로 시작", TimelineColors.Fixed, Modifier.weight(1f)) {
                    onChoose(BlockKind.FIXED)
                }
                KindOption("자유 블록", "메모만 남겨요\n비워두면 빈칸", TimelineColors.Free, Modifier.weight(1f)) {
                    onChoose(BlockKind.FREE)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun KindOption(title: String, desc: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, TimelineColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
        Text(desc, fontSize = 12.sp, color = Color(0xFF424242))
    }
}

/* ───────────────────────── Preview ───────────────────────── */

@Preview(showBackground = true, widthDp = 360, heightDp = 760, name = "1일차 · 현재 14:10")
@Composable
private fun TimelinePreview() {
    TimelineScreen(now = LocalTime.of(14, 10))
}
